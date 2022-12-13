/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.anomaly_models.univariate.UnivariateOutlierModel;
import com.rapidminer.extension.anomalydetection.metadata.UnivariateOutlierModelMetaData;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.studio.internal.Resources;


public class DetectUnivariateOutliers extends Operator {


	public InputPort exaInput = getInputPorts().createPort("exa", ExampleSet.class);

	public OutputPort exaOutput = getOutputPorts().createPort("outlier");

	public OutputPort modOutput = getOutputPorts().createPort("mod");


	public static final String PARAMETER_METHOD = "method";
	public static final String PARAMETER_AGGREGATION_METHOD = "aggregation_method";
	public static final String PARAMETER_CREATE_INDIVIDUAL_SCORES = "show_individual_scores";
	public static final String PARAMETER_THRESHOLD = "percentile_threshold";
	public static final String PARAMETER_SCORING_TYPE = "scoring_type";

	public static final String PERCENTILE_DISTANCE = "Percentile Distance";
	public static final String QUARTILES = "Quartiles";
	public static final String HISTOGRAM = "Histgoram";
	public static final String ZSCORE = "z-Score";
	public static String[] supportedAlgorithms = {PERCENTILE_DISTANCE,QUARTILES, HISTOGRAM, ZSCORE};
	public static String[] supportedAggregations = {"Average", "Maximum", "Product"};
	// only used in percentile threshold
	public static String[] scoringModes = new String[]{AnomalyUtilities.SCORING_MODE_BOTH,AnomalyUtilities.SCORING_MODE_ONLY_BOTTOM,AnomalyUtilities.SCORING_MODE_ONLY_TOP};

	private final TableSubsetSelector attributeSelector = new TableSubsetSelector(this, exaInput, ValueTypeColumnFilter.TYPE_REAL, ValueTypeColumnFilter.TYPE_INTEGER);

	public DetectUnivariateOutliers(OperatorDescription description) throws IncompatibleMDClassException {
		super(description);


		getTransformer().addRule(() -> {
			try {
				UnivariateOutlierModelMetaData md = null;

				md = new UnivariateOutlierModelMetaData(
						exaInput.getMetaData(TableMetaData.class));

				if (exaInput.isConnected()) {
					TableMetaData subset = attributeSelector.getMetaDataSubset(exaInput.getMetaData(TableMetaData.class), false);
					if (getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES)) {
						md.addIndividualScores(new ArrayList<>(subset.labels()));
					}
				}
				modOutput.deliverMD(
						md);
			} catch (IncompatibleMDClassException e) {
				e.printStackTrace();
			}
		});

		getTransformer().addRule(() -> {
			try {
				UnivariateOutlierModelMetaData md = new UnivariateOutlierModelMetaData(
						exaInput.getMetaData(TableMetaData.class));
				if (exaInput.isConnected()) {
					TableMetaData subset = attributeSelector.getMetaDataSubset(exaInput.getMetaData(TableMetaData.class), false);
					if (getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES)) {
						md.addIndividualScores(new ArrayList<>(subset.labels()));
					}
				}
				TableMetaData tmd = exaInput.getMetaData(TableMetaData.class);
				TableMetaData resultMD = md.apply(tmd, exaInput);

				exaOutput.deliverMD(resultMD);
			} catch (IncompatibleMDClassException e) {
				e.printStackTrace();
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {
		IOTable inputTable = exaInput.getData(IOTable.class);
		ConcurrencyContext concurrencyContext = Resources.getConcurrencyContext(this);
		Context beltContext = ContextAdapter.adapt(concurrencyContext);

		UnivariateOutlierModel model = new UnivariateOutlierModel(
				inputTable,
				getParameterAsString(PARAMETER_METHOD),
				getParameterAsString(PARAMETER_AGGREGATION_METHOD),
				getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES),
				getParameterAsDouble(PARAMETER_THRESHOLD),
				getParameterAsString(PARAMETER_SCORING_TYPE)
		);

		List<String> trainingColumns = new ArrayList<>();
		for (String att : attributeSelector.getSubset(inputTable.getTable(), false).labels()) {
//			if (!att.isNumerical()) {
//				throw new UserError(this, 104, this.getName(), att.getName());
//			}
			trainingColumns.add(att);
		}

		model.learnOnBelt(inputTable.getTable(), trainingColumns, beltContext);
		IOTable scoredTable = model.apply(inputTable, this);

//		visOutput.deliver(model.getExplainPredictionsIOObject());
		exaOutput.deliver(scoredTable);
		modOutput.deliver(model);

	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSelector.getParameterTypes());
		types.add(new ParameterTypeCategory(PARAMETER_METHOD,
				"Outlier detection method to use", supportedAlgorithms, 1));
		// Percentile Distance
		ParameterTypeDouble threshold = new ParameterTypeDouble(PARAMETER_THRESHOLD,"Percentile Threshold",0,0.5,0.05);
		threshold.registerDependencyCondition(new EqualStringCondition(this,PARAMETER_METHOD,true, PERCENTILE_DISTANCE));
		types.add(threshold);
		ParameterTypeCategory scoringModeParameter = new ParameterTypeCategory(
				PARAMETER_SCORING_TYPE,"defines what kind of anomalies can be found",scoringModes,0,true);
		scoringModeParameter.registerDependencyCondition(new EqualStringCondition(this,PARAMETER_METHOD,true, PERCENTILE_DISTANCE));
		types.add(scoringModeParameter);
		//
		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION_METHOD,
				"What method to use to create a single outlier score", supportedAggregations, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES, "if set to true the operator will create" +
				"one score per attribute", false));

		return types;
	}


}
