package com.rapidminer.extension.anomalydetection.anomaly_models.statistical;


import java.util.Arrays;
import java.util.Map;
import java.util.SplittableRandom;
import java.util.logging.Logger;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.ExecutionUtils;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.reader.MixedRowReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.extension.anomalydetection.anomaly_models.IOTableAnomalyModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.belt.BeltTools;


/**
 * @author mschmitz
 * @since 3.4.0
 */
public class IsolationForestModel extends IOTableAnomalyModel {

	private static final String AVERAGE_PATH = "average_path";
	private static final String NORMALIZED_SCORE = "normalized_score";
	public static final String[] AVAILABLE_SCORING_MODES = new String[]{AVERAGE_PATH,
			NORMALIZED_SCORE};

	private final IsolationForestNode[] rootNodes;

	private final String scoringMode;
	private final int trainingSize;
	private final double scoreNormalizationConstant;
	private static final long serialVersionUID = 6463454137845839353L;

	/**
	 * Only used for JSON-deserialization
	 */
	public IsolationForestModel() {
		rootNodes = null;
		scoringMode = null;
		trainingSize = 0;
		scoreNormalizationConstant = 0;

	}

	/**
	 * Builds an iForest with given parameters
	 *
	 * @param ioTable     the data to fit the model on
	 * @param nTrees      number of trees
	 * @param maxLeafSize size when the splitting stops.
	 * @param context     belt context for computation
	 */
	public IsolationForestModel(IOTable ioTable, int nTrees, int maxLeafSize, int maxFeatures, double bootstrapRatio, String scoringMode,
								Context context, Operator operator) throws OperatorException {

		super(ioTable, Tables.ColumnSetRequirement.SUPERSET, Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
		Table table = ioTable.getTable();
		this.scoringMode = scoringMode;
		trainingSize = table.height();
		// needed if you want to use the normalized score later.
		double H = Math.log(trainingSize - 1.0) + .5772156649;
		scoreNormalizationConstant = 2 * H - 2 * ((double) trainingSize - 1) / (double) trainingSize;
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

		int bootstrappedSamples = (int) Math.round(bootstrapRatio * table.height());
		ExecutionUtils.parallel(0, nTrees, i -> {
			IsolationForestNode tree = new IsolationForestNode(maxLeafSize, maxFeatures, treeRngs[i]);
			try {
				Context treeContext = new SequentialContext();
				Table bootstrappedTable = AnomalyUtilities.bootStrapTable(table, bootstrappedSamples, treeRngs[i], treeContext);
				tree.fit(bootstrappedTable, treeContext);
				rootNodes[i] = tree;
				operator.getProgress().step();
			} catch (OperatorException e) {
				Logger.getLogger(IsolationForestModel.class.getName()).warning(e.getMessage());
			}

		}, context);
	}


	@Override
	protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
		ConcurrencyContext context = new SequentialConcurrencyContext();
		MixedRowReader rowReader = Readers.mixedRowReader(adapted);
		NumericBuffer buffer = Buffers.realBuffer(adapted.height());

		int rowId = 0;
		while (rowReader.hasRemaining()) {
			rowReader.move();
			double treepathLength = 0;
			for (IsolationForestNode node : rootNodes) {
				treepathLength += node.apply(rowReader, adapted.labels());
			}
			double averageLength = treepathLength / (double) rootNodes.length;
			double score;
			switch (scoringMode) {
				case NORMALIZED_SCORE:
					score = Math.pow(2, -1 * (averageLength / scoreNormalizationConstant));
					break;
				default:
					score = averageLength;
			}
			buffer.set(rowId, score);
			rowId++;
		}
		return buffer.toColumn();
	}

	public String toString() {
		return "IsolationForest model";
	}

}

