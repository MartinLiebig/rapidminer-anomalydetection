package com.rapidminer.extension.anomalydetection.metadata;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.extension.anomalydetection.anomaly_models.univariate.UnivariateOutlierModel;
import com.rapidminer.extension.anomalydetection.operator.utility.flag_generator.ThresholdFlagModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.AbstractIOTableModel;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;


public class UnivariateOutlierModelMetaData extends TableModelMetaData {
	TableMetaData trainingMetaData;
	boolean showIndividualScores = false;

	public UnivariateOutlierModelMetaData() {
		super();
	}

	public UnivariateOutlierModelMetaData(UnivariateOutlierModel model, boolean shortend) {
		super(model.getClass(), (TableMetaData) MetaData.forIOObject(model.getTrainingHeader(), shortend),
				GeneralModel.ModelKind.PREPROCESSING);

	}

	public UnivariateOutlierModelMetaData(TableMetaData tableMetaData){
		super(UnivariateOutlierModel.class, tableMetaData,GeneralModel.ModelKind.PREPROCESSING);
	};

	@Override
	protected TableMetaData applyEffects(TableMetaData tmd, InputPort inputPort) {
		TableMetaDataBuilder tableMetaDataBuilder = new TableMetaDataBuilder(tmd);
		ColumnInfoBuilder columnInfoBuilder = new ColumnInfoBuilder(ColumnType.REAL);
		tableMetaDataBuilder.add(Attributes.PREDICTION_NAME, columnInfoBuilder.build()).addColumnMetaData(Attributes.PREDICTION_NAME, ColumnRole.PREDICTION);
		if(showIndividualScores){
			appendIndividualScores(tableMetaDataBuilder);
		}
		return tableMetaDataBuilder.build();
	}

	public void addIndividualScores(TableMetaData tmd){
		this.trainingMetaData = tmd;
		showIndividualScores=true;
	}

	public void appendIndividualScores(TableMetaDataBuilder tableMetaDataBuilder){
		for(String columnName : trainingMetaData.labels()){
			ColumnInfoBuilder columnInfoBuilder = new ColumnInfoBuilder(ColumnType.REAL);
			String name = Attributes.PREDICTION_NAME + "(" + columnName + ")";
			tableMetaDataBuilder.add(name, columnInfoBuilder.build())
					.addColumnMetaData(name, ColumnRole.PREDICTION);;
		}
	}

}
