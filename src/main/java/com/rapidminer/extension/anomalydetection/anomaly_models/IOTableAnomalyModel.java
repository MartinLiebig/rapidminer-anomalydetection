package com.rapidminer.extension.anomalydetection.anomaly_models;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.operator.learner.IOTablePredictionModel;


public abstract class IOTableAnomalyModel extends IOTablePredictionModel {

	public IOTableAnomalyModel() {
	}

	public IOTableAnomalyModel(IOTable trainingTable, Tables.ColumnSetRequirement columnSetRequirement,
							   Tables.TypeRequirement... typeRequirements) {
		super(trainingTable, columnSetRequirement, typeRequirements);
	}

	@Override
	protected Column getLabelColumn() {
		return null;
	}

	@Override
	public String getLabelName() {
		return null;
	}

}
