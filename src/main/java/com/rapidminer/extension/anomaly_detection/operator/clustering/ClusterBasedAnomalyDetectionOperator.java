package com.rapidminer.extension.anomaly_detection.operator.clustering;

import java.util.LinkedList;
import java.util.List;

import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomaly_detection.model.AnomalyDetectionModel;
import com.rapidminer.extension.anomaly_detection.model.clustering.CBLOFModel;
import com.rapidminer.extension.anomaly_detection.model.clustering.CMGOSModel;
import com.rapidminer.extension.anomaly_detection.model.clustering.LDCOFModel;
import com.rapidminer.extension.anomaly_detection.operator.AbstractAnomalyOperator;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorCapability;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.CapabilityCheck;
import com.rapidminer.operator.learner.CapabilityProvider;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.EqualTypeCondition;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.ParameterService;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.Tools;
import com.rapidminer.tools.math.similarity.DistanceMeasure;
import com.rapidminer.tools.math.similarity.DistanceMeasureHelper;
import com.rapidminer.tools.math.similarity.DistanceMeasures;
import com.rapidminer.tools.math.similarity.numerical.EuclideanDistance;


public class ClusterBasedAnomalyDetectionOperator extends AbstractAnomalyOperator {

	public static final String PARAMETER_ALGORITHM = "algorithm";
	private final static String CBLOF = "CBLOF";
	private final static String CMGOS = "CMGOS";
	private final static String LDCOF = "LDCOF";
	public static final String[] AVAILABLE_ALGORITHMS = new String[]{CBLOF, CMGOS, LDCOF};

	// --- CBLOF Specific Parameters --- //
	///alpha specifies the percentage of normal data
	public static String PARAMETER_ALPHA = "alpha";
	///The minimum ratio between a normal and anomalous cluster
	public static String PARAMETER_BETA = "beta";
	///The Parameter name for &quot; Uses the cluster size as a weight factor as proposed by the original publication.
	public static String PARAMETER_WEIGHTING = "use cluster size as weighting factor";

	public static String PARAMETER_LIKE_CBLOF = "divide clusters like cblof";

	/**
	 * Parameter name for gamma &quot; ratio between the maximum size of small clusters and the average cluster size
	 * &quot. Small clusters are removed.;
	 **/
	public static String PARAMETER_GAMMA_LDCOF = "gamma_(ldcof)";


	/**
	 * // --- CMGOS Specific Parameters --- ///
	 **/

	public static final String[] COV = new String[]{
			"Reduction",
			"Regularisation",
			"MCD"
	};

	public static final int METHOD_COV_REDUCTION = 0;
	public static final int METHOD_COV_REGULARIZE = 1;
	public static final int METHOD_COV_MCD = 2;

	public static final String PARAMETER_LAMBDA = "lambda";
	public static final String PARAMETER_LAMBDA_DESCRIPTION = "Lambda for regularization (see Friedmann). A lambda of 0.0 menas QDA (each cluster has its own covariance) and a lambda of 1.0 means LDA (a global covariance matrix).";

	/**
	 *
	 **/
	public static final String PARAMETER_COVARIANCE = "covariance estimation";
	public static final String PARAMETER_COVARIANCE_DESCRIPTION = "The algorithm to estimate the covariance matrics. Reduction is the simplest method whereas the other two are more complex. Details can be found in the papers (see Operator description).";
	/**
	 *
	 **/
	public static final String PARAMETER_H = "h (non-outlier instances)";
	public static final String PARAMETER_H_DESCRIPTION = "This parameter specifies the number of samples for fastMCD/MCD to be used for a computation (non-outliers). If set to -1 it is automatically computed according to the 'probability for normal class'. Friedmann et al recommend to use 75% of the examples as a good estimate. The upper bound is the numer of examples and the lower bound is (number of examples * dimensions +1)/2. Values exceeding these limits will be replaced by the limit. ";
	/**
	 *
	 **/
	public static final String PARAMETER_NUMBER_OF_SUBSETS = "number of subsets";
	public static final String PARAMETER_POINTS_SUBSET_DESCRIPTION = "Defines the number of subsets used in fastMCD. Friedmann recommends to have at most 5 subsets.";
	/**
	 *
	 **/
	public static final String PARAMETER_FMCD = "threshold for fastMCD";
	public static final String PARAMETER_FMCD_DESCRIPTION = "If the number of examples in the dataset exceeds the threshold, fastMCD will be applied instead of MCD (complete search). Not recommended to be higher than 600 due to computational issues.";
	/**
	 *
	 **/
	public static final String PARAMETER_RUN = "iterations";
	public static final String PARAMETER_RUN_DESCRIPTION = "Number of iterations for computing the MCD. 100-500 might be a good choice.";

	/**
	 * The parameter name for &quot;Specifies the number of threads for execution.&quot; Specifies that evaluation
	 * process should be performed in parallel &quot;
	 **/
	public static final String PARAMETER_NUMBER_OF_THREADS = "number of threads";
	public static final String PARAMETER_NUMBER_OF_THREADS_DESCRIPTION = "The number of threads for the computation";
	/**
	 * The number of times outlier should be removed for minimum covariance determinant
	 */
	public static final String PARAMETER_NUMBER_OF_REMOVE = "times to remove outlier";
	public static final String PARAMETER_NUMBER_OF_REMOVE_DESCRIPTION = "The number of times outlier should be removed for minimum covariance determinant";
	/**
	 * The normal class probability
	 */
	public static final String PARAMETER_OUTLIER_PROBABILITY = "probability for normal class";
	public static final String PARAMETER_OUTLIER_PROBABILITY_DESCRIPTION = "This is the expected probability of normal data instances. Usually it should be between 0.95 and 1.0 to make sense.";
	/**
	 * Boolean to use a given number of points to calculate the covariance matrix
	 */
	public static final String PARAMETER_LIMIT_COVARIANCE_POINTS = "limit computations";
	public static final String PARAMETER_LIMIT_COVARIANCE_POINTS_DESCRIPTION = "Limit the number of instances to calculate the covariance matrix. Should be used for very large clusters. The sampling of the instances is a random choice.";
	/**
	 * Number of points for covariance matrix calculation
	 */
	public static final String PARAMETER_NUMBER_COVARIANCE_POINTS = "maximum";
	public static final String PARAMETER_NUMBER_COVARIANCE_POINTS_DESCRIPTION = "Maximum number of instances for covariance matrix calculation";
	/**
	 * Boolean for parallelization
	 */
	public static final String PARAMETER_PARALLELIZE_EVALUATION_PROCESS = "parallelize evaluation process";
	public static final String PARAMETER_PARALLELIZE_EVALUATION_PROCESS_DESCRIPTION = "Specifies that evaluation process should be performed in parallel";
	/**
	 * Parameter name for gamma &quot; ratio between the maximum size of small clusters and the average cluster size
	 * &quot. Small clusters are removed.;
	 **/
	public static String PARAMETER_GAMMA = "gamma";


	protected InputPort clusterInput = getInputPorts().createPort("clu", ClusterModel.class);
	protected OutputPort clusterThroughput = getOutputPorts().createPort("clu");

	private DistanceMeasureHelper measureHelper = new DistanceMeasureHelper(
			this);


	public ClusterBasedAnomalyDetectionOperator(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(clusterInput, clusterThroughput);

	}

	public void doWork() throws OperatorException {
		ExampleSet exampleSet = exaInput.getData(ExampleSet.class);

		CapabilityCheck check = new CapabilityCheck(this, Tools.booleanValue(
				ParameterService.getParameterValue(CapabilityProvider.PROPERTY_RAPIDMINER_GENERAL_CAPABILITIES_WARN), true));
		check.checkLearnerCapabilities(this, exampleSet);

		AnomalyDetectionModel model;
		String usedAlgorithm = getParameterAsString(PARAMETER_ALGORITHM);
		DistanceMeasure measure = measureHelper.getInitializedMeasure(exampleSet);
		switch (usedAlgorithm) {
			case CBLOF:
				model = buildCBLOF(exampleSet, measure);
				break;
			case LDCOF:
				model = buildLDCOF(exampleSet, measure);
				break;
			case CMGOS:
				model = buildCMGOS(exampleSet, measure);
				break;
			default:
				throw new OperatorException("Unknown algorithm " + usedAlgorithm);
		}

		ExampleSet resultSet = model.apply(exampleSet);

		exaOutput.deliver(resultSet);
		modOutput.deliver(model);
		clusterThroughput.deliver(clusterInput.getData(ClusterModel.class));

	}

	protected AnomalyDetectionModel buildCBLOF(ExampleSet trainingSet, DistanceMeasure measure) throws OperatorException {
		ClusterModel mod = clusterInput.getData(ClusterModel.class);
		CBLOFModel anomalyModel = new CBLOFModel(trainingSet, mod, measure);
		anomalyModel.setAlpha(getParameterAsDouble(PARAMETER_ALPHA));
		anomalyModel.setBeta(getParameterAsDouble(PARAMETER_BETA));
		anomalyModel.setUseClusterWeights(getParameterAsBoolean(PARAMETER_WEIGHTING));
		return anomalyModel;
	}

	protected AnomalyDetectionModel buildLDCOF(ExampleSet trainingSet, DistanceMeasure measure) throws OperatorException {
		ClusterModel mod = clusterInput.getData(ClusterModel.class);

		LDCOFModel anomalyModel;
		if (getParameterAsBoolean(PARAMETER_LIKE_CBLOF)) {
			anomalyModel = new LDCOFModel(trainingSet, mod, measure);
			anomalyModel.setUseGamma(true);
			anomalyModel.setGamma(getParameterAsDouble(PARAMETER_GAMMA_LDCOF));
		} else {
			anomalyModel = new LDCOFModel(trainingSet, mod, new EuclideanDistance());
			anomalyModel.setAlpha(getParameterAsDouble(PARAMETER_ALPHA));
			anomalyModel.setBeta(getParameterAsDouble(PARAMETER_BETA));
		}
		return anomalyModel;
	}

	protected AnomalyDetectionModel buildCMGOS(ExampleSet trainingSet, DistanceMeasure measure) throws OperatorException {
		ClusterModel mod = clusterInput.getData(ClusterModel.class);
		CMGOSModel anomalyModel = new CMGOSModel(trainingSet, mod, measure);
		anomalyModel.setThreads(1);
		anomalyModel.setRemoveRuns(getParameterAsInt(PARAMETER_NUMBER_OF_REMOVE));
		anomalyModel.setProbability(getParameterAsDouble(PARAMETER_OUTLIER_PROBABILITY));

		int variancePoints = -1;
		if (getParameterAsBoolean(PARAMETER_LIMIT_COVARIANCE_POINTS)) {
			variancePoints = getParameterAsInt(PARAMETER_NUMBER_COVARIANCE_POINTS);
		}
		if (getParameterAsBoolean(PARAMETER_LIMIT_COVARIANCE_POINTS + "_")) {
			variancePoints = getParameterAsInt(PARAMETER_NUMBER_COVARIANCE_POINTS + "_");
		}
		anomalyModel.setCov_sampling(variancePoints);

		anomalyModel.setPercentage(getParameterAsDouble(PARAMETER_GAMMA));
		anomalyModel.setLambda(getParameterAsDouble(PARAMETER_LAMBDA));
		anomalyModel.setCov(getParameterAsInt(PARAMETER_COVARIANCE));
		anomalyModel.setH(getParameterAsInt(PARAMETER_H));
		anomalyModel.setNumberOfSubsets(getParameterAsInt(PARAMETER_NUMBER_OF_SUBSETS));
		anomalyModel.setFastMCDPoints(getParameterAsInt(PARAMETER_FMCD));
		anomalyModel.setInititeration(getParameterAsInt(PARAMETER_RUN));
		RandomGenerator generator = RandomGenerator.getRandomGenerator(this);
		anomalyModel.setRandomGenerator(generator);

		return anomalyModel;
	}


	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();


		ParameterType algorithm = new ParameterTypeCategory(PARAMETER_ALGORITHM, "which algorithm to choose", AVAILABLE_ALGORITHMS, 0, false);
		types.add(algorithm);

		List<ParameterType> distancetypes = DistanceMeasures.getParameterTypes(this);
		types.addAll(distancetypes);

		EqualStringCondition isCBLOForLDCOF = new EqualStringCondition(this, PARAMETER_ALGORITHM, true, CBLOF, LDCOF);
		// CBLOF specific
		ParameterType alpha = new ParameterTypeDouble(PARAMETER_ALPHA, "This parameter specifies the percentage of the data set that is expected to be normal",
				0, 100, 90, false);
		alpha.registerDependencyCondition(isCBLOForLDCOF);

		ParameterType beta = new ParameterTypeDouble(
				PARAMETER_BETA,
				"This parameter specifies the minimum ratio between the size of a large cluster and a small cluster",
				1, Integer.MAX_VALUE, 5, false);
		beta.registerDependencyCondition(isCBLOForLDCOF);

		ParameterType weights = new ParameterTypeBoolean(
				PARAMETER_WEIGHTING,
				"Uses the cluster size as a weight factor as proposed by the original publication.",
				true);
		weights.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_ALGORITHM, true, CBLOF));

		ParameterType likeCBLOF = new ParameterTypeBoolean(
				PARAMETER_LIKE_CBLOF,
				"The division into large and small clusters will be implemented in a manner similar to CBLOF.",
				false, false);
		EqualStringCondition isLDCOF = new EqualStringCondition(this,PARAMETER_ALGORITHM,true,LDCOF);
		likeCBLOF.registerDependencyCondition(isLDCOF);
		types.add(likeCBLOF);

		ParameterType gamma = new ParameterTypeDouble(
				PARAMETER_GAMMA_LDCOF,
				"ratio between the maximum size of small clusters and the average cluster size",
				0, 1, 0.1);
		// TODO: Make alpha and beta disappear, if this condition is fullfilled AND its LDCOF
		gamma.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIKE_CBLOF, true, false));
		gamma.registerDependencyCondition(isLDCOF);
		types.add(alpha);
		types.add(beta);
		types.add(gamma);
		types.add(weights);


		List<ParameterType> CMGOSParameters = getCMGOSParameters();
		for (ParameterType p : CMGOSParameters) {
			p.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_ALGORITHM, true, CMGOS));
		}
		types.addAll(CMGOSParameters);


		return types;
	}

	private List<ParameterType> getCMGOSParameters() {
		List<ParameterType> types = new LinkedList<ParameterType>();

		types.add(new ParameterTypeDouble(PARAMETER_OUTLIER_PROBABILITY, PARAMETER_OUTLIER_PROBABILITY_DESCRIPTION, 0, 1.0, 0.975, false));
		types.add(new ParameterTypeDouble(PARAMETER_GAMMA, "Ratio between the maximum size of small clusters and the average cluster size. Small" +
				"clusters are removed.",
				0, 1, 0.1));


		types.add(new ParameterTypeCategory(PARAMETER_COVARIANCE, PARAMETER_COVARIANCE_DESCRIPTION, COV, 0, false));

		ParameterTypeInt type2 = new ParameterTypeInt(PARAMETER_NUMBER_OF_REMOVE, PARAMETER_NUMBER_OF_REMOVE_DESCRIPTION, 0, Integer.MAX_VALUE, 1, false);
		type2.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_REDUCTION, METHOD_COV_REGULARIZE));
		types.add(type2);

		ParameterTypeBoolean type3 = (new ParameterTypeBoolean(PARAMETER_LIMIT_COVARIANCE_POINTS, PARAMETER_LIMIT_COVARIANCE_POINTS_DESCRIPTION, false, false));
		type3.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, 1));
		types.add(type3);
		ParameterTypeInt type = (new ParameterTypeInt(PARAMETER_NUMBER_COVARIANCE_POINTS, PARAMETER_NUMBER_COVARIANCE_POINTS_DESCRIPTION, 1, Integer.MAX_VALUE, 1000, false));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_COVARIANCE_POINTS, true, true));
		types.add(type);
		type3 = (new ParameterTypeBoolean(PARAMETER_LIMIT_COVARIANCE_POINTS + "_", PARAMETER_LIMIT_COVARIANCE_POINTS_DESCRIPTION, false, false));
		type3.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, 0));
		types.add(type3);
		type = (new ParameterTypeInt(PARAMETER_NUMBER_COVARIANCE_POINTS + "_", PARAMETER_NUMBER_COVARIANCE_POINTS_DESCRIPTION, 1, Integer.MAX_VALUE, 1000, false));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_LIMIT_COVARIANCE_POINTS + "_", true, true));
		types.add(type);
		type = (new ParameterTypeInt(PARAMETER_H, PARAMETER_H_DESCRIPTION, 0, Integer.MAX_VALUE, -1, false));
		type.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_MCD));
		types.add(type);
		type = (new ParameterTypeInt(PARAMETER_RUN, PARAMETER_RUN_DESCRIPTION, 0, Integer.MAX_VALUE, 500, false));
		type.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_MCD));
		types.add(type);
		type = (new ParameterTypeInt(PARAMETER_FMCD, PARAMETER_FMCD_DESCRIPTION, 0, Integer.MAX_VALUE, 600, false));
		type.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_MCD));
		types.add(type);
		type = (new ParameterTypeInt(PARAMETER_NUMBER_OF_SUBSETS, PARAMETER_POINTS_SUBSET_DESCRIPTION, 0, Integer.MAX_VALUE, 5, false));
		type.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_MCD));
		types.add(type);

		ParameterTypeDouble type1 = (new ParameterTypeDouble(PARAMETER_LAMBDA, PARAMETER_LAMBDA_DESCRIPTION, 0, 1, 0.1, false));
		type1.registerDependencyCondition(new EqualTypeCondition(getParameterHandler(), PARAMETER_COVARIANCE, COV, false, METHOD_COV_REGULARIZE));
		types.add(type1);

//		List<ParameterType> distancetypes = DistanceMeasures.getParameterTypes(this);
//		types.addAll(distancetypes);

		types.add(new ParameterTypeBoolean(PARAMETER_PARALLELIZE_EVALUATION_PROCESS, PARAMETER_PARALLELIZE_EVALUATION_PROCESS_DESCRIPTION, false, false));
		type = (new ParameterTypeInt(PARAMETER_NUMBER_OF_THREADS, PARAMETER_NUMBER_OF_THREADS_DESCRIPTION, 1, Integer.MAX_VALUE, Runtime.getRuntime().availableProcessors(), false));
		type.registerDependencyCondition(new BooleanParameterCondition(this, PARAMETER_PARALLELIZE_EVALUATION_PROCESS, true, true));
		types.add(type);

		types.addAll(RandomGenerator.getRandomGeneratorParameters(this));
		return types;
	}



}
