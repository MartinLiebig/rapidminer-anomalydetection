package com.rapidminer.extension.anomalydetection.model.statistical;


import java.util.Arrays;
import java.util.SplittableRandom;
import java.util.logging.Logger;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.ExecutionUtils;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.model.IOTableAnomalyModel;

import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.belt.BeltTools;


/**
 * @author mschmitz
 * @since 2.10.0
 */
public class IsolationForestModel extends IOTableAnomalyModel {

	private static final String AVERAGE_PATH = "average_path";
	private static final String NORMALIZED_SCORE = "normalized_score";
	public static final String[] AVAILABLE_SCORING_MODES = new String[] { AVERAGE_PATH,
			NORMALIZED_SCORE };

	private final IsolationForestNode[] rootNodes;

	private final String scoringMode;
	private final int trainingSize;
	private final double scoreNormalizationConstant;
	private static final long serialVersionUID = 6463454137845839353L;

	/**
	 * Builds an iForest with given parameters
	 *
	 * @param table
	 * 		the data to fit the model on
	 * @param nTrees
	 * 		number of trees
	 * @param maxLeafSize
	 * 		size when the splitting stops.
	 * @param context
	 * 		belt context for computation
	 */
	public IsolationForestModel(Table table, int nTrees, int maxLeafSize, String scoringMode,
								Context context, Operator operator) throws OperatorException {
		super(TableViewCreator.INSTANCE.convertOnWriteView(new IOTable(table), false));

		this.scoringMode = scoringMode;
		trainingSize = table.height();
		// needed if you want to use the normalized score later.
		double H = Math.log(trainingSize - 1.0) + .5772156649;
		scoreNormalizationConstant = 2 * H - 2 * ( (double) trainingSize - 1 ) / (double) trainingSize;
		for (String name : BeltTools.selectRegularColumns(table).labels()) {
			if (BeltTools.containsMissingValues(table.column(name))) {
				throw new UserError(operator, 139, "Isolation Forest");
			}
			if (BeltTools.containsNonFiniteValues(table.column(name))) {
				throw new UserError(operator, "infinite_values", "Isolation Forest");
			}
		}
		if (BeltTools.containsColumnType(BeltTools.regularSubtable(table),
				Column.TypeId.DATE_TIME)) {
			throw new UserError(operator, "operator_toolbox.outlier.date_not_supported");
		}

		operator.getProgress().setTotal(nTrees);
		// fit the trees
		rootNodes = new IsolationForestNode[nTrees];
		SplittableRandom root = new SplittableRandom(
				RandomGenerator.getGlobalRandomGenerator().nextLong());
		SplittableRandom[] treeRngs = new SplittableRandom[nTrees];
		Arrays.setAll(treeRngs, i -> root.split());

		ExecutionUtils.parallel(0, nTrees, i -> {
			IsolationForestNode tree = new IsolationForestNode(maxLeafSize, treeRngs[i]);
			try {
				tree.fit(table, new SequentialContext());
				rootNodes[i] = tree;
				operator.getProgress().step();
			} catch (OperatorException e) {
				Logger.getLogger(IsolationForestModel.class.getName()).warning(e.getMessage());
			}

		}, context);
	}

	/**
	 * Apply the mode to a given ES. Just calls the performPredictionModel
	 *
	 * @param exampleSet
	 * 		the Examplest to apply the model on. Needs to be a superset of the training data
	 * @return a scored example set.
	 */
	@Override
	public ExampleSet apply(ExampleSet exampleSet) {
		ConcurrencyContext context = new SequentialConcurrencyContext();
		Table ioTable = BeltConverter.convert(exampleSet, context).getTable();
		IOTable resulttable = apply(ioTable);
		return BeltConverter.convert(resulttable, context);
	}

	/**
	 * Score a given table. This is preferred since you do not need to convert ExampleSets to
	 * Tables
	 *
	 * @param table
	 * 		the table to score
	 * @return a scored table.
	 */
	@Override
	public IOTable apply(Table table) {
		ConcurrencyContext context = new SequentialConcurrencyContext();
		MixedRowReader rowReader = Readers.mixedRowReader(table);
		NumericBuffer buffer = Buffers.realBuffer(table.height());

		int rowId = 0;
		while (rowReader.hasRemaining()) {
			rowReader.move();
			double treepathLength = 0;
			for (IsolationForestNode node : rootNodes) {
				treepathLength += node.apply(rowReader, table.labels());
			}
			double averageLength = treepathLength / (double) rootNodes.length;
			double score;
			switch (scoringMode) {
				case NORMALIZED_SCORE:
					score = Math.pow(2, -1 * ( averageLength / scoreNormalizationConstant ));
					break;
				default:
					score = averageLength;
			}
			buffer.set(rowId, score);
			rowId++;
		}

		Table resulttable = Builders.newTableBuilder(table)
				.add(OUTLIER_SCORE_NAME, buffer.toColumn())
				.addMetaData(OUTLIER_SCORE_NAME, ColumnRole.OUTLIER)
				.build(ContextAdapter.adapt(context));
		return new IOTable(resulttable);
	}

	public String toString() {
		return "IsolationForest model";
	}

}

