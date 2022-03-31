package com.rapidminer.extension.anomalydetection.operator.utility.flag_generator;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.IOTablePreprocessingModel;
import com.rapidminer.studio.internal.Resources;


public class ThresholdFlagModel extends IOTablePreprocessingModel {
	private double threshold;
	private String scoreName;

	public ThresholdFlagModel() {
	}

	public ThresholdFlagModel(IOTable trainingTable, double threshold) {
		super(trainingTable);
		this.threshold = threshold;
		this.scoreName = trainingTable.getTable().select().withMetaData(ColumnRole.SCORE).labels().get(0);

	}

	public ThresholdFlagModel(IOTable trainingTable, double threshold, String scoreColumnName) {
		super(trainingTable);
		this.threshold = threshold;
		this.scoreName = scoreColumnName;
	}


	@Override
	public void applyOnData(Table adjusted, TableBuilder builder, Operator operator) throws OperatorException {

		ConcurrencyContext concurrencyContext = Resources.getConcurrencyContext(operator);
		Context beltContext = ContextAdapter.adapt(concurrencyContext);

		Column flagColumn = adjusted.transform(scoreName).applyNumericToNominal(score -> score > threshold
						? AnomalyUtilities.ANOMALY_NAME : AnomalyUtilities.NO_ANOMALY_NAME,
				2, beltContext).toColumn();

		builder.add(AnomalyUtilities.ANOMALY_FLAG_NAME, flagColumn)
				.addMetaData(AnomalyUtilities.ANOMALY_FLAG_NAME, ColumnRole.OUTLIER);

	}


	@Override
	protected boolean needsRemapping() {
		return false;
	}
}
