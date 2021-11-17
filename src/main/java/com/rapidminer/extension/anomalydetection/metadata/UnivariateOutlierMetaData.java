package com.rapidminer.extension.anomalydetection.metadata;

import com.rapidminer.example.Attributes;
import com.rapidminer.extension.anomalydetection.model.univariate.UnivariateOutlierModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.Model;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ModelMetaData;
import com.rapidminer.tools.Ontology;


public class UnivariateOutlierMetaData extends ModelMetaData {
	public UnivariateOutlierMetaData() {
		super();
	}

	public UnivariateOutlierMetaData(UnivariateOutlierModel model, boolean shortend) {
		super(model, shortend);

	}
	public UnivariateOutlierMetaData(Class<? extends Model> mclass, ExampleSetMetaData trainingSetMetaData,
						 GeneralModel.ModelKind... modelKinds) {
		super(mclass, trainingSetMetaData, modelKinds);
	}




	protected ExampleSetMetaData applyEffects(ExampleSetMetaData emd, InputPort inputPort) {
		emd.addAttribute(new AttributeMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME, Ontology.REAL, Attributes.CONFIDENCE_NAME));
		return
				emd;
	}


}
