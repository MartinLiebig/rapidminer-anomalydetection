package com.rapidminer.extension.anomalydetection.metadata;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.extension.anomalydetection.anomaly_models.IOTableAnomalyModel;
import com.rapidminer.extension.anomalydetection.anomaly_models.univariate.UnivariateOutlierModel;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;


public class AnomalyModelMetaData extends TableModelMetaData {


	public AnomalyModelMetaData() {
		super();
	}

	public AnomalyModelMetaData(IOTableAnomalyModel model, boolean shortend) {
		super(model.getClass(), (TableMetaData) MetaData.forIOObject(model.getTrainingHeader(), shortend),
				GeneralModel.ModelKind.UNSUPERVISED);


	}

	public AnomalyModelMetaData(TableMetaData tableMetaData) {
		super(IOTableAnomalyModel.class, tableMetaData, GeneralModel.ModelKind.UNSUPERVISED);
	}

	;

	@Override
	protected TableMetaData applyEffects(TableMetaData tmd, InputPort inputPort) {
		TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(tmd);
		ColumnInfoBuilder columnInfoBuilder = new ColumnInfoBuilder(ColumnType.REAL);
		tableMetaDataBuilder.add(Attributes.PREDICTION_NAME, columnInfoBuilder.build()).addColumnMetaData(Attributes.PREDICTION_NAME, ColumnRole.SCORE);

		return tableMetaDataBuilder.build();
	}


	@Override
	public AnomalyModelMetaData clone() {
		AnomalyModelMetaData myMetadata = (AnomalyModelMetaData) super.clone();
		return myMetadata;
	}

}
