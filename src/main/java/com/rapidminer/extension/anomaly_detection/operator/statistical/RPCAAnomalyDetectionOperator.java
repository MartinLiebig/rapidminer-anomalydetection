package com.rapidminer.extension.anomaly_detection.operator.statistical;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomaly_detection.model.statistical.RPCAModel;
import com.rapidminer.extension.anomaly_detection.operator.AbstractAnomalyOperator;
import com.rapidminer.operator.*;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.OperatorService;

import java.util.List;

public class RPCAAnomalyDetectionOperator extends AbstractAnomalyOperator {

    public static final String PARAMETER_REDUCTION_TYPE = "component_usage";
    public static final String PARAMETER_REDUCTION_TYPE_DESCRIPTION = "Select wich principal components should be used for anomaly score computation. Major PCs are typically preferred in literature.";
    public static final String[] PCS_METHODS = new String[] {
            "use all components",
            "only use major components",
            "only use minor components",
            "use major and minor components"
    };

    public static final int PCS_ALL   = 0;
    public static final int PCS_TOP   = 1;
    public static final int PCS_LOWER = 2;
    public static final int PCS_BOTH  = 3;

    public static final String PARAMETER_TOP_METHODS = "major_components";
    public static final String PARAMETER_TOP_METHODS_DESCRIPTION = "Select method for major principal components to use.";
    public static final String[] PCS_TOP_METHODS = new String[] {
            "use variance threshold",
            "use fixed number of components",
    };

    public static final int PCS_TOP_VAR   = 0;
    public static final int PCS_TOP_FIX   = 1;

    public static final String PARAMETER_LOW_METHODS = "minor_components";
    public static final String PARAMETER_LOW_METHODS_DESCRIPTION = "Select method for minor principal components to use.";
    public static final String[] PCS_LOW_METHODS = new String[] {
            "use max eigenvalue",
            "use fixed number of components",
    };

    public static final int PCS_LOW_VAL   = 0;
    public static final int PCS_LOW_FIX   = 1;


    /**
     * The normal class probability; used for robustness and chi**2 dist normalization.
     */
    public static final String PARAMETER_OUTLIER_PROBABILITY = "probability_for_normal_class";
    public static final String PARAMETER_OUTLIER_PROBABILITY_DESCRIPTION = "This is the expected probability of normal data instances. Usually it should be between 0.95 and 1.0.";

    public static final String PARAMETER_VARIANCE_THRESHOLD = "cumulative_variance";
    public static final String PARAMETER_VARIANCE_THRESHOLD_DESCRIPTION = "Cumulative variance threshold for selecting major components.";

    public static final String PARAMETER_NUMBER_OF_COMPONENTS_TOP = "number_of_major_pcs";
    public static final String PARAMETER_NUMBER_OF_COMPONENTS_TOP_DESCRIPTION = "Number of major components to keep.";

    public static final String PARAMETER_NUMBER_OF_COMPONENTS_LOW = "number_of_minor_pcs";
    public static final String PARAMETER_NUMBER_OF_COMPONENTS_LOW_DESCRIPTION = "Number of minor components to keep.";

    public static final String PARAMETER_VALUE_THRESHOLD = "eigenvalue_threshold_max";
    public static final String PARAMETER_VALUE_THRESHOLD_DESCRIPTION = "The maximum allowed eigenvalue for minor components taken into account.";


    public RPCAAnomalyDetectionOperator(OperatorDescription description) {
        super(description);
    }

    public void doWork() throws OperatorException {
        ExampleSet trainset = exaInput.getData(ExampleSet.class);
        RPCAModel rpcaModel = new RPCAModel(trainset);

        // set parameters
        rpcaModel.setNormProb(getParameterAsDouble(PARAMETER_OUTLIER_PROBABILITY));
        rpcaModel.setReductionType(getParameterAsInt(PARAMETER_REDUCTION_TYPE));
        rpcaModel.setTopmethod(getParameterAsInt(PARAMETER_TOP_METHODS));
        rpcaModel.setVarianceThreshold(getParameterAsDouble(PARAMETER_VARIANCE_THRESHOLD));
        rpcaModel.setNumberOfTopComponents(getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS_TOP));

        rpcaModel.setLowMethod(getParameterAsInt(PARAMETER_LOW_METHODS));
        rpcaModel.setValueThreshold(getParameterAsDouble(PARAMETER_VALUE_THRESHOLD));

        rpcaModel.setNumberOfLowComponents(getParameterAsInt(PARAMETER_NUMBER_OF_COMPONENTS_LOW));

        rpcaModel.train(trainset);
        exaOutput.deliver(rpcaModel.apply(trainset));
        modOutput.deliver(rpcaModel);
    }
    @Override
    public List<ParameterType> getParameterTypes() {
        List<ParameterType> list = super.getParameterTypes();

        list.add(new ParameterTypeDouble(PARAMETER_OUTLIER_PROBABILITY, PARAMETER_OUTLIER_PROBABILITY_DESCRIPTION, 0, 1.0, 0.975, false));

        ParameterType type = new ParameterTypeCategory(PARAMETER_REDUCTION_TYPE, PARAMETER_REDUCTION_TYPE_DESCRIPTION, PCS_METHODS, PCS_ALL);
        type.setExpert(false);
        list.add(type);

        type = new ParameterTypeCategory(PARAMETER_TOP_METHODS, PARAMETER_TOP_METHODS_DESCRIPTION, PCS_TOP_METHODS, PCS_TOP_VAR);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, false, PCS_TOP, PCS_BOTH));
        list.add(type);

        type = new ParameterTypeDouble(PARAMETER_VARIANCE_THRESHOLD, PARAMETER_VARIANCE_THRESHOLD_DESCRIPTION, Double.MIN_VALUE, 1, 0.50);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, true, PCS_TOP, PCS_BOTH));
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_TOP_METHODS, PCS_TOP_METHODS, true, PCS_TOP_VAR));
        list.add(type);

        type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS_TOP, PARAMETER_NUMBER_OF_COMPONENTS_TOP_DESCRIPTION, 1, Integer.MAX_VALUE, 1);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, true, PCS_TOP, PCS_BOTH));
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_TOP_METHODS, PCS_TOP_METHODS, true, PCS_TOP_FIX));
        list.add(type);

        type = new ParameterTypeCategory(PARAMETER_LOW_METHODS, PARAMETER_LOW_METHODS_DESCRIPTION, PCS_LOW_METHODS, PCS_LOW_VAL);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, false, PCS_LOWER, PCS_BOTH));
        list.add(type);

        type = new ParameterTypeDouble(PARAMETER_VALUE_THRESHOLD, PARAMETER_VALUE_THRESHOLD_DESCRIPTION, 0, Double.MAX_VALUE, 0.20);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, true, PCS_LOWER, PCS_BOTH));
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_LOW_METHODS, PCS_LOW_METHODS, true, PCS_LOW_VAL));
        list.add(type);

        type = new ParameterTypeInt(PARAMETER_NUMBER_OF_COMPONENTS_LOW, PARAMETER_NUMBER_OF_COMPONENTS_LOW_DESCRIPTION, 1, Integer.MAX_VALUE, 1);
        type.setExpert(false);
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_REDUCTION_TYPE, PCS_METHODS, true, PCS_LOWER, PCS_BOTH));
        type.registerDependencyCondition(new EqualTypeCondition(this, PARAMETER_LOW_METHODS, PCS_LOW_METHODS, true, PCS_LOW_FIX));
        list.add(type);

        return list;
    }


}
