package com.rapidminer.extension.anomalydetection.model.statistical;


import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.apache.commons.collections4.list.SetUniqueList;
import org.apache.poi.poifs.filesystem.NPOIFSDocument;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.Workload;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.table.Table;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.belt.BeltTools;

/**
 *
 * @author mschmitz
 * @since 2.10.0
 */
public class IsolationForestNode implements Serializable {

	private static final long serialVersionUID = -6361565464734538899L;

	private IsolationForestNode leftChild = null;
	private IsolationForestNode rightChild = null;

	private final transient SplittableRandom randomGenerator;
	private double threshold = 0.5;
	private String splitString = "";
	private final int maxLeafSize;
	private int maxFeatures;

	private String columnName;

	private boolean isNominal = false;

	private List<String> possibleLabels = null;

	public IsolationForestNode(int maxLeafSize, int maxFeatures, SplittableRandom random) {
		this.maxLeafSize = maxLeafSize;
		this.randomGenerator = random;
		this.maxFeatures = maxFeatures;
	}

	public IsolationForestNode(int maxLeafSize,int maxFeatures, List possibleLabels, SplittableRandom random) {
		this.maxLeafSize = maxLeafSize;
		this.randomGenerator = random;
		this.maxFeatures = maxFeatures;
		this.possibleLabels = possibleLabels;
	}

	public void fit(Table table, Context context) throws OperatorException {
		if( possibleLabels == null){
			possibleLabels = new ArrayList<>();
			List<String> labels = BeltTools.selectRegularColumns(table).labels();
			while(possibleLabels.size()<maxFeatures){
				String label = labels.get(randomGenerator.nextInt(labels.size()));
				if(!possibleLabels.contains(label))
					possibleLabels.add(label);
			}
		}
		columnName = possibleLabels.get(randomGenerator.nextInt(possibleLabels.size()));

		if (table.column(columnName).type().category().equals(Column.Category.NUMERIC)) {
			splitNumerical(table, context);
		} else {
			if (table.column(columnName).type().category().equals(Column.Category.CATEGORICAL)) {
				splitNominal(table, context);
			} else {
				throw new OperatorException(
						"Isolation forests can only handle numerical and nominal columns. " + columnName + " is neither");
			}
		}

	}

	private void splitNumerical(Table table, Context context) throws OperatorException {
		double min = table.transform(columnName)
				.reduceNumeric(Double.MAX_VALUE, Double :: min, context);
		double max = table.transform(columnName)
				.reduceNumeric(Double.MIN_VALUE, Double :: max, context);

		// We got a constant. We stop to grow here.
		if (min != max) {

			threshold = randomGenerator.nextDouble(min, max);

			Table left = table.filterNumeric(columnName, value -> value > threshold,
					Workload.DEFAULT, context);
			Table right = table.filterNumeric(columnName, value -> value <= threshold,
					Workload.DEFAULT, context);
			fitChild(context, left, right);
		}
	}

	private void splitNominal(Table table, Context context) throws OperatorException {
		isNominal = true;

		Column c = table.column(columnName);
		Statistics.CategoricalIndexCounts indexCounts = com.rapidminer.belt.column.Statistics.compute(
				c, Statistics.Statistic.INDEX_COUNTS, context)
				.getObject(
						Statistics.CategoricalIndexCounts.class);
		// list of available non-zero indices
		List<Integer> nonZeroIndices = new ArrayList<>();
		for (int i = 0; i < c.getDictionary().maximalIndex(); ++i) {
			if (indexCounts.countForIndex(i) > 0) { nonZeroIndices.add(i); }
		}
		if (nonZeroIndices.size() > 1) {
			int r = randomGenerator.nextInt(0, nonZeroIndices.size());
			int dictIndex = nonZeroIndices.get(r);
			splitString = c.getDictionary().get(dictIndex);

			Table left = table.filterCategorical(columnName, value -> value == dictIndex,
					Workload.DEFAULT, context);
			Table right = table.filterCategorical(columnName, value -> value != dictIndex,
					Workload.DEFAULT, context);

			fitChild(context, left, right);
		}
	}

	private void fitChild(Context context, Table left, Table right) throws OperatorException {
		if (left.height() > maxLeafSize) {
			leftChild = new IsolationForestNode(maxLeafSize,maxFeatures,possibleLabels, randomGenerator);
			leftChild.fit(left, context);
		}
		if (right.height() > maxLeafSize) {
			rightChild = new IsolationForestNode(maxLeafSize,maxFeatures, possibleLabels, randomGenerator);
			rightChild.fit(right, context);
		}
	}

	public int apply(MixedRowReader numericReader, List<String> labels) {
		IsolationForestNode chosenChild;
		int columnid = labels.indexOf(columnName);
		if (!isNominal) {
			double value = numericReader.getNumeric(columnid);
			chosenChild = value > threshold ? leftChild : rightChild;
		} else {
			String value = numericReader.getObject(columnid, String.class);
			chosenChild = value.equals(splitString) ? leftChild : rightChild;
		}

		if (chosenChild == null) {
			return 1;
		} else {
			return chosenChild.apply(numericReader, labels) + 1;
		}
	}

	public String toString() {
		return toString(0, 5);
	}

	private String toString(int level, int maxLevel) {
		StringBuilder s = new StringBuilder();
		for (int i = 0; i < level; ++i) {
			s.append("--");
		}
		if (leftChild != null || rightChild != null) {
			if (isNominal) {
				s.append(columnName).append("==").append(splitString);
			} else {
				s.append(columnName).append(">=").append(threshold);
			}
			if (level < maxLevel) {
				s.append("\n");
				if (leftChild != null) {
					s.append(leftChild.toString(level + 1, maxLevel));
				}
				if (rightChild != null) {
					s.append(rightChild.toString(level + 1, maxLevel));
				}
			}
		}

		return s.toString();
	}

}
