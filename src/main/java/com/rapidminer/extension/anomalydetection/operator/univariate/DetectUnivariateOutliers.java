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
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.anomaly_models.univariate.UnivariateOutlierModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.TableModelMetaData;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.studio.internal.Resources;


public class DetectUnivariateOutliers extends Operator {


	public InputPort exaInput = getInputPorts().createPort("exa", ExampleSet.class);

	public OutputPort exaOutput = getOutputPorts().createPort("outlier");

	public OutputPort modOutput = getOutputPorts().createPort("mod");


	public static final String PARAMETER_METHOD = "method";
	public static final String PARAMETER_AGGREGATION_METHOD = "aggregation_method";
	public static final String PARAMETER_CREATE_INDIVIDUAL_SCORES = "show_individual_scores";

	public static String[] supportedAlgorithms = {"Quartiles", "Histogram", "z-Score"};
	public static String[] supportedAggregations = {"Average", "Maximum", "Product"};

	private final TableSubsetSelector attributeSelector = new TableSubsetSelector(this, exaInput, ValueTypeColumnFilter.TYPE_REAL, ValueTypeColumnFilter.TYPE_INTEGER);

	public DetectUnivariateOutliers(OperatorDescription description) {
		super(description);


		getTransformer().addRule(() -> {
			try {
				modOutput.deliverMD(
					new TableModelMetaData(UnivariateOutlierModel.class,exaInput.getMetaData(TableMetaData.class)));
			} catch (IncompatibleMDClassException e) {
				e.printStackTrace();
			}
			});

		getTransformer().addRule(() -> {
			try {
				TableMetaData tmd = exaInput.getMetaData(TableMetaData.class);
				TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
				ColumnInfoBuilder columnInfoBuilder =
						new ColumnInfoBuilder(ColumnType.REAL);
				builder.add(AnomalyUtilities.ANOMALY_SCORE_NAME, columnInfoBuilder.build()).addColumnMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME, ColumnRole.SCORE);
				exaOutput.deliverMD(builder.build());
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
				getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES)
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
				"Outlier detection method to use", supportedAlgorithms, 0));
		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION_METHOD,
				"What method to use to create a single outlier score", supportedAggregations, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES, "if set to true the operator will create" +
				"one score per attribute", false));

		return types;
	}


}
