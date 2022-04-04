package com.rapidminer.extension.anomalydetection.metadata;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.extension.anomalydetection.anomaly_models.univariate.UnivariateOutlierModel;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.IOTableModel;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.MetaData;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;


public class UnivariateOutlierModelMetaData extends TableModelMetaData {
	public List<String> trainingColumns;
	public boolean showIndividualScores = false;

	public UnivariateOutlierModelMetaData() {
		super();
	}

	public UnivariateOutlierModelMetaData(UnivariateOutlierModel model, boolean shortend) {
		super(model.getClass(), (TableMetaData) MetaData.forIOObject(model.getTrainingHeader(), shortend),
				GeneralModel.ModelKind.PREPROCESSING);
		this.showIndividualScores = model.getShowScores();
		this.trainingColumns = model.getTrainingColumns();

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
			appendIndividualScoresToTable(tableMetaDataBuilder);
		}
		return tableMetaDataBuilder.build();
	}

	public void addIndividualScores(List<String> trainingColumns){
		this.trainingColumns = trainingColumns;
		showIndividualScores=true;
	}

	public void appendIndividualScoresToTable(TableMetaDataBuilder tableMetaDataBuilder){
		for(String columnName : trainingColumns){
			ColumnInfoBuilder columnInfoBuilder = new ColumnInfoBuilder(ColumnType.REAL);
			String name = Attributes.PREDICTION_NAME + "(" + columnName + ")";
			tableMetaDataBuilder.add(name, columnInfoBuilder.build())
					.addColumnMetaData(name, ColumnRole.PREDICTION);;
		}
	}

	@Override
	public UnivariateOutlierModelMetaData clone(){
		UnivariateOutlierModelMetaData myMetadata = (UnivariateOutlierModelMetaData) super.clone();
		myMetadata.trainingColumns = new ArrayList<>(this.trainingColumns);
		myMetadata.showIndividualScores = this.showIndividualScores;
		return myMetadata;
	}

}
