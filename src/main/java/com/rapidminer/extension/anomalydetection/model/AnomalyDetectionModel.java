package com.rapidminer.extension.anomalydetection.model;

import java.io.Serializable;
import java.util.Locale;

import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.RemappedExampleSet;
import com.rapidminer.example.table.AttributeFactory;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.AbstractModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Ontology;

@Deprecated
public abstract class AnomalyDetectionModel extends AbstractModel implements Serializable {

	protected AnomalyDetectionModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	@Override
	public ExampleSet apply(ExampleSet testSet) throws OperatorException {
		Attribute scoreAttribute = addAnomalyAttribute(testSet);
		ExampleSet mappedExampleSet = RemappedExampleSet.create(testSet, getTrainingHeader(), false, true);
		double[] scores = evaluate(mappedExampleSet);
		int i = 0;
		for(Example e : testSet){
			e.setValue(scoreAttribute,scores[i]);
			i++;
		}
		return testSet;
	}

	public abstract double[] evaluate(ExampleSet testSet) throws OperatorException;

	public Attribute addAnomalyAttribute(ExampleSet exampleSet) {
		Attribute anomalyScore = AttributeFactory.createAttribute(
				AnomalyUtilities.ANOMALY_SCORE_NAME, Ontology.REAL);
		exampleSet.getExampleTable().addAttribute(anomalyScore);
		exampleSet.getAttributes().setSpecialAttribute(anomalyScore, Attributes.CONFIDENCE_NAME);
		return anomalyScore;
	}
}
