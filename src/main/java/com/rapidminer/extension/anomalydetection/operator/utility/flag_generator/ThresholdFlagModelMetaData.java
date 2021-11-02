package com.rapidminer.extension.anomalydetection.operator.utility.flag_generator;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.GeneralModelMetaData;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;


public class ThresholdFlagModelMetaData extends TableModelMetaData {
	public ThresholdFlagModelMetaData(){};

	public ThresholdFlagModelMetaData(ThresholdFlagModel model, boolean ignoreStatistics){
		super(model.getClass(), (TableMetaData) MetaData.forIOObject(model.getTrainingHeader(), ignoreStatistics),
				GeneralModelMetaData.modelKindsAsArray(model));
	};
	@Override
	protected TableMetaData applyEffects(TableMetaData tmd, InputPort inputPort) {
		TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
		ColumnInfoBuilder columnInfoBuilder =
				new ColumnInfoBuilder(ColumnType.NOMINAL)
						.setBooleanDictionaryValues(AnomalyUtilities.ANOMALY_NAME, AnomalyUtilities.NO_ANOMALY_NAME);
		builder.add(AnomalyUtilities.ANOMALY_FLAG_NAME, columnInfoBuilder.build()).addColumnMetaData(AnomalyUtilities.ANOMALY_FLAG_NAME, ColumnRole.PREDICTION);
		return builder.build();
	}
}
