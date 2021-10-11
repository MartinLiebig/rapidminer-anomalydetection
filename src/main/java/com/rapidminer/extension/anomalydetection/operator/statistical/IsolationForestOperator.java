package com.rapidminer.extension.anomalydetection.operator.statistical;




import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.util.ColumnRole;
//import com.rapidminer.extension.operator_toolbox.operator.outliers.OutlierModelMetaData;
import com.rapidminer.extension.anomalydetection.model.statistical.IsolationForestModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.belt.BeltTools;

/**
 *
 * @author mschmitz
 * @since 2.10.0
 */
public class IsolationForestOperator extends Operator implements CapabilityProvider {

	private final InputPort exaInput = getInputPorts().createPort("exa");

	private final OutputPort exaOuput = getOutputPorts().createPort("exa");
	private final OutputPort modOutput = getOutputPorts().createPort("mod");

	public static final String PARAMETER_N_TRESS = "number_of_trees";
	public static final String PARAMETER_MAX_LEAF_SIZE = "max_leaf_size";
	public static final String PARAMETER_SCORE_CALCULATION = "score_calculation";

	public IsolationForestOperator(OperatorDescription description) {
		super(description);

		getTransformer().addGenerationRule(modOutput, IsolationForestModel.class);
//		getTransformer().addRule(() -> {
//			OutlierModelMetaData ommd = new OutlierModelMetaData(
//					(ExampleSetMetaData) exaInput.getMetaData());
//			modOutput.deliverMD(ommd);
//		});

		getTransformer().addRule(
				new ExampleSetPassThroughRule(exaInput, exaOuput, SetRelation.EQUAL) {

					@Override
					public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {

						metaData.addAttribute(new AttributeMetaData("outlier_score", Ontology.REAL,
								ColumnRole.OUTLIER.toString()));

						return metaData;
					}
				});
	}

	@Override
	public void doWork() throws OperatorException {
		IOTable wrapper = exaInput.getData(IOTable.class);

		Table table = wrapper.getTable();

		Context context = new SequentialContext();

		IsolationForestModel forest = new IsolationForestModel(table,
				getParameterAsInt(PARAMETER_N_TRESS),
				getParameterAsInt(
						PARAMETER_MAX_LEAF_SIZE),
				getParameterAsString(
						PARAMETER_SCORE_CALCULATION),
				context, this);

		IOTable result = forest.apply(table);
		exaOuput.deliver(result);
		modOutput.deliver(forest);
	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeInt(PARAMETER_N_TRESS, "Number of trees in the forest", 1,
				Integer.MAX_VALUE, 100));
		types.add(
				new ParameterTypeInt(PARAMETER_MAX_LEAF_SIZE, "Number of examples in the last leaf",
						1, Integer.MAX_VALUE, 1));
		types.add(
				new ParameterTypeCategory(PARAMETER_SCORE_CALCULATION, "How to calculate the score",
						IsolationForestModel.AVAILABLE_SCORING_MODES, 0));
		return types;
	}

	@Override
	public boolean supportsCapability(OperatorCapability capability) {
		switch (capability) {
			case NUMERICAL_ATTRIBUTES:
			case POLYNOMINAL_LABEL:
			case BINOMINAL_LABEL:
			case BINOMINAL_ATTRIBUTES:
			case NUMERICAL_LABEL:
			case NO_LABEL:
			case POLYNOMINAL_ATTRIBUTES:
				return true;
			case MISSING_VALUES:
			case WEIGHTED_EXAMPLES:
				return false;
			default:
				return false;
		}
	}
}

