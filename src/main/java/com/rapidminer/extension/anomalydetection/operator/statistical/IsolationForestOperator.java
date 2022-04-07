package com.rapidminer.extension.anomalydetection.operator.statistical;


import java.util.List;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Table;

import com.rapidminer.example.Attributes;
import com.rapidminer.extension.anomalydetection.anomaly_models.statistical.IsolationForestModel;
import com.rapidminer.extension.anomalydetection.operator.AbstractAnomalyOperator;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.belt.BeltTools;


/**
 * @author mschmitz
 * @since 2.10.0
 */
public class IsolationForestOperator extends AbstractAnomalyOperator implements CapabilityProvider {

	public static final String PARAMETER_N_TRESS = "number_of_trees";
	public static final String PARAMETER_MAX_LEAF_SIZE = "max_leaf_size";
	public static final String PARAMETER_MAX_FEATURES = "max_features";
	public static final String PARAMETER_AUTO_FEATURE_SIZE = "use_feature_heuristic";
	public static final String PARAMETER_SCORE_CALCULATION = "score_calculation";
	public static final String PARAMETER_BOOTSTRAP_RATIO = "bootstrap_ratio";

	public IsolationForestOperator(OperatorDescription description) {
		super(description);



	}

	@Override
	public void doWork() throws OperatorException {
		IOTable ioTable = exaInput.getData(IOTable.class);

		Table table = ioTable.getTable();

		Context context = BeltTools.getContext(this);

		int maxFeatures;
		int regularWidth = Math.round(BeltTools.regularSubtable(table).width());
		if (getParameterAsBoolean(PARAMETER_AUTO_FEATURE_SIZE)) {
			maxFeatures = regularWidth;
		}
		else{
			maxFeatures = getParameterAsInt(PARAMETER_MAX_FEATURES);
		}
		if(maxFeatures>regularWidth){
			throw new UserError(this,"anomaly_detection.example_error",maxFeatures,regularWidth);
		}

		IsolationForestModel forest = new IsolationForestModel(ioTable,
				getParameterAsInt(PARAMETER_N_TRESS),
				getParameterAsInt(PARAMETER_MAX_LEAF_SIZE),
				maxFeatures,
				getParameterAsDouble(PARAMETER_BOOTSTRAP_RATIO),
				getParameterAsString(
						PARAMETER_SCORE_CALCULATION),
				context, this);

		IOTable result = forest.apply(ioTable,this);
		exaOutput.deliver(result);
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
		types.add(new ParameterTypeDouble(PARAMETER_BOOTSTRAP_RATIO,"defines what fraction of the data set is used for bootstrapping in each tree",0,Double.MAX_VALUE,0.9));
		ParameterType useFeatureHeuristic = new ParameterTypeBoolean(PARAMETER_AUTO_FEATURE_SIZE,PARAMETER_AUTO_FEATURE_SIZE,true);
		ParameterType maxFeatures =				new ParameterTypeInt(PARAMETER_MAX_FEATURES, "Number of features per tree",
				1, Integer.MAX_VALUE, 5);

		maxFeatures.registerDependencyCondition(new BooleanParameterCondition(this,PARAMETER_AUTO_FEATURE_SIZE,true,false));
		types.add(useFeatureHeuristic);
		types.add(maxFeatures);

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

