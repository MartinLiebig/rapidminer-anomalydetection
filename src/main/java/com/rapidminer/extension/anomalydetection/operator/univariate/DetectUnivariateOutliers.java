/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.model.univariate.UnivariateOutlierModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.operator.tools.AttributeSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.Ontology;


public class DetectUnivariateOutliers extends Operator {


	public InputPort exaInput = getInputPorts().createPort("exa", ExampleSet.class);

	public OutputPort exaOutput = getOutputPorts().createPort("outlier");
	//public OutputPort visOutput = getOutputPorts().createPort("vis");
	public OutputPort modOutput = getOutputPorts().createPort("mod");


	public static final String PARAMETER_METHOD = "method";
	public static final String PARAMETER_AGGREGATION_METHOD = "aggregation_method";
	public static final String PARAMETER_CREATE_INDIVIDUAL_SCORES = "show_individual_scores";

	public static String[] supportedAlgorithms = {"Quartiles", "Histogram", "z-Score"};
	public static String[] supportedAggregations = {"Average", "Maximum", "Product"};

	protected final AttributeSubsetSelector attributeSelector = new AttributeSubsetSelector(this, exaInput,
			Ontology.REAL, Ontology.INTEGER, Ontology.NUMERICAL);

	public DetectUnivariateOutliers(OperatorDescription description) {
		super(description);
		//getTransformer().addGenerationRule(visOutput, ExplainPredictionsIOObject.class);
		getTransformer().addGenerationRule(modOutput, PreprocessingModel.class);
		// TODO: make this proper
		getTransformer().addPassThroughRule(exaInput, exaOutput);
	}

	@Override
	public void doWork() throws OperatorException {
		ExampleSet inputTable = exaInput.getData(ExampleSet.class);
		ConcurrencyContext concurrencyContext = Resources.getConcurrencyContext(this);
		Context beltContext = ContextAdapter.adapt(concurrencyContext);

		UnivariateOutlierModel model = new UnivariateOutlierModel(
				inputTable,
				getParameterAsString(PARAMETER_METHOD),
				getParameterAsString(PARAMETER_AGGREGATION_METHOD),
				getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES)
		);

		Set<Attribute> s = attributeSelector.getAttributeSubset(inputTable, false, false);
		List<String> trainingColumns = new ArrayList<>();
		for (Attribute att : s) {
			if (!att.isNumerical()) {
				throw new UserError(this, 104, this.getName(), att.getName());
			}
			trainingColumns.add(att.getName());
		}
		IOTable t = BeltConverter.convert(inputTable, concurrencyContext);
		model.learnOnBelt(t.getTable(), trainingColumns, beltContext);
		Table scoredTable = model.scoreTable(t.getTable(), true, beltContext);

//		visOutput.deliver(model.getExplainPredictionsIOObject());
		exaOutput.deliver(new IOTable(scoredTable));
		modOutput.deliver(model);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSelector.getParameterTypes());
		types.add(new ParameterTypeCategory(PARAMETER_METHOD,
				"Outlier detection method to use", supportedAlgorithms, 0));
		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION_METHOD,
				"What method to use to create a single outlier score", supportedAggregations, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES, "if set to true the operator will create" +
				"one score per attribute", false));

		return types;
	}


}
