package com.rapidminer.extension.anomalydetection.operator.time_series;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.extension.anomalydetection.operator.time_series.algorithm.AbstractTSOutlierDetector;
import com.rapidminer.extension.anomalydetection.operator.time_series.algorithm.LinearRegressionOutlierDetector;
import com.rapidminer.extension.anomalydetection.operator.time_series.algorithm.StdOutlierDetector;
import com.rapidminer.extension.anomalydetection.operator.time_series.algorithm.TSOutlierCapability;
import com.rapidminer.extension.anomalydetection.operator.time_series.algorithm.ZScoreOutlierDetector;
import com.rapidminer.extension.anomalydetection.operator.univariate.DetectUnivariateOutliers;

import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.AverageScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.MaxScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ProductScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ScoreAggregation;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.AttributeMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetMetaData;
import com.rapidminer.operator.ports.metadata.ExampleSetPassThroughRule;
import com.rapidminer.operator.ports.metadata.SetRelation;
import com.rapidminer.operator.preprocessing.filter.columns.ValueTypeColumnFilter;
import com.rapidminer.operator.tools.TableSubsetSelector;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeInt;
import com.rapidminer.parameter.UndefinedParameterError;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.parameter.conditions.PortConnectedCondition;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.Ontology;


/**
 * An operator to detect outlier in a time series it supports two kind of modes 1. If a dataset is connected at the ref
 * port it will compare a given test window with the reference window and calcualte a score 2. If no dataset is given it
 * will take a trainwindow and compare it to the test window which is right next to it.
 */
public class DetectTimeSeriesOutliers extends Operator {

	public InputPort exaInput = getInputPorts().createPort("exa", IOTable.class);
	public InputPort refInput = getInputPorts().createPort("ref");

	public OutputPort exaOutput = getOutputPorts().createPort("scored");
	public OutputPort oriOutput = getOutputPorts().createPort("ori");


	public static final String PARAMETER_TRAINING_SIZE = "training_size";
	public static final String PARAMETER_TEST_SIZE = "test_size";
	public static final String PARAMETER_METHOD = "method";
	public static final String PARAMETER_AGGREGATION_METHODS = DetectUnivariateOutliers.PARAMETER_AGGREGATION_METHOD;
	public static final String PARAMETER_CREATE_INDIVIDUAL_SCORES = DetectUnivariateOutliers.PARAMETER_CREATE_INDIVIDUAL_SCORES;
	public static final String PARAMETER_SHOW_ADVANCED_SETTINGS = "show_advanced_settings";
	public static final String PARAMETER_USE_ABSOLUTES_FOR_AGGREGATION = "use_absolutes_in_aggregation";

	private static final String ZSCORE = ZScoreOutlierDetector.METHOD_NAME;
	private static final String LINEAR_REGRESSION = LinearRegressionOutlierDetector.METHOD_NAME;
	private static final String STD_DEV = StdOutlierDetector.METHOD_NAME;

	public static final String[] AVAILABLE_METHODS = {LINEAR_REGRESSION, STD_DEV, ZSCORE};
	public static final String[] AVAILABLE_AGGREGATIONS = DetectUnivariateOutliers.supportedAggregations;

	private final TableSubsetSelector attributeSelector = new TableSubsetSelector(this, exaInput, ValueTypeColumnFilter.TYPE_REAL, ValueTypeColumnFilter.TYPE_INTEGER);

	public DetectTimeSeriesOutliers(OperatorDescription description) {
		super(description);

		getTransformer().addRule(new ExampleSetPassThroughRule(exaInput, exaOutput, SetRelation.EQUAL) {

			@Override
			public ExampleSetMetaData modifyExampleSet(ExampleSetMetaData metaData) {
				metaData.addAttribute(new AttributeMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME, Ontology.REAL, ColumnRole.SCORE.toString().toLowerCase()));
				return metaData;
			}
		});
	}

	@Override
	public void doWork() throws OperatorException {

		IOTable ioTable = exaInput.getData(IOTable.class);
		Table table = ioTable.getTable();
		Table referenceTable = null;
		if (refInput.isConnected()) {
			referenceTable = refInput.getData(IOTable.class).getTable();
		}

		Context context = ContextAdapter.adapt(Resources.getConcurrencyContext(this));

		TableBuilder builder = Builders.newTableBuilder(table);
		if (table.labels().contains(AnomalyUtilities.ANOMALY_SCORE_NAME)) {
			builder.remove(AnomalyUtilities.ANOMALY_SCORE_NAME);
		}
		// create the individual scores
		List<NumericBuffer> scoreBuffers = new ArrayList<>();
		Table subset = attributeSelector.getSubset(table,false);
		for (String columnName : subset.labels()) {
			NumericBuffer results;
			if (refInput.isConnected()) {
				results = applyWithTrainingSet(Objects.requireNonNull(referenceTable), table, columnName);
			} else {
				results = applyWithSlidingWindow(table, columnName);
			}
			scoreBuffers.add(results);
			if(getParameterAsBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES)) {
				builder.add(AnomalyUtilities.ANOMALY_SCORE_NAME+"_" + columnName, results.toColumn())
						.addMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME+"_" + columnName, ColumnRole.SCORE);
			}
		}
		// Create an aggregated score
		NumericBuffer aggregatedScoreBuffer = Buffers.realBuffer(table.height());
		for (int rowCounter = 0; rowCounter < table.height(); rowCounter++) {
			ScoreAggregation scoreAggregation = getScoreAggregation(getParameterAsBoolean(PARAMETER_USE_ABSOLUTES_FOR_AGGREGATION));
			for (NumericBuffer scoreBuffer : scoreBuffers) {
				double value = scoreBuffer.get(rowCounter);
				scoreAggregation.addValue(value);
			}
			aggregatedScoreBuffer.set(rowCounter, scoreAggregation.getAggregate());

		}
		builder.add(AnomalyUtilities.ANOMALY_SCORE_NAME, aggregatedScoreBuffer.toColumn())
				.addMetaData(AnomalyUtilities.ANOMALY_SCORE_NAME, ColumnRole.SCORE);
		exaOutput.deliver(new IOTable(builder.build(context)));


	}

	/**
	 * Applies the method in a sliding window fashion. The windows are defined from the end of the series, so that you
	 * have missing values in the beginning.
	 *
	 * @param table      the table to generate the score on
	 * @param ColumnName the name of the column to analyze
	 * @return a buffer with outlier scores for each row (excluding the first rows)
	 * @throws UserError if the method does not support a windowed approach
	 */
	private NumericBuffer applyWithSlidingWindow(Table table, String ColumnName) throws UserError {
		int trainSize = getParameterAsInt(PARAMETER_TRAINING_SIZE);
		int testSize = getParameterAsInt(PARAMETER_TEST_SIZE);
		NumericBuffer buffer = Buffers.realBuffer(table.column(ColumnName));
		NumericBuffer results = Buffers.realBuffer(buffer.size());
		AbstractTSOutlierDetector outlierDetector = createOutlierDetector();
		if (!outlierDetector.supportsCapability(TSOutlierCapability.SUPPORTS_SLIDING_WINDOW)) {
			throw new UserError(this, "operator_toolbox.outlier.does_not_support_window", getParameterAsString(PARAMETER_METHOD));
		}

		for (int testWindowEnd = buffer.size(); testWindowEnd - testSize - trainSize >= 0; testWindowEnd -= testSize) {

			int testWindowStart = testWindowEnd - testSize;
			int trainWindowEnd = testWindowStart;
			int trainWindowStart = trainWindowEnd - trainSize;

			double[] trainingWindowData = new double[trainSize];
			int arrayPosition = 0;
			for (int trainWindowPosition = trainWindowStart; trainWindowPosition < trainWindowEnd; trainWindowPosition++) {
				trainingWindowData[arrayPosition] = buffer.get(trainWindowPosition);
				arrayPosition++;
			}
			double[] testWindowData = new double[testSize];
			arrayPosition = 0;
			for (int testWindowPosition = testWindowStart; testWindowPosition < testWindowEnd; testWindowPosition++) {
				testWindowData[arrayPosition] = buffer.get(testWindowPosition);
				arrayPosition++;
			}

			outlierDetector.train(trainingWindowData);
			double[] scores = outlierDetector.apply(testWindowData);

			arrayPosition = 0;
			for (int testWindowPosition = testWindowStart; testWindowPosition < testWindowEnd; testWindowPosition++) {
				results.set(testWindowPosition, scores[arrayPosition]);
				arrayPosition++;
			}
		}
		return results;
	}

	/**
	 * Calculates scores of the testing table and takes trainingTable as the reference set.
	 *
	 * @param trainingTable the reference data table
	 * @param testingTable  the table you want to get the score off
	 * @param columnName    the column name to analyze
	 * @return a buffer with outlier scores for each row (sometimes excluding the first rows)
	 * @throws UserError if the algorithm does not support a training set approach (i.e. forecasting approaches).
	 */
	private NumericBuffer applyWithTrainingSet(Table trainingTable, Table testingTable, String columnName) throws UserError {
		NumericBuffer trainingBuffer = Buffers.realBuffer(trainingTable.column(columnName));
		NumericBuffer results = Buffers.realBuffer(trainingBuffer.size());
		NumericBuffer testingBuffer = Buffers.realBuffer(testingTable.column(columnName));
		//
		// Train the model
		//
		AbstractTSOutlierDetector outlierDetector = createOutlierDetector();
		if (!outlierDetector.supportsCapability(TSOutlierCapability.SUPPORTS_TRAINING)) {
			throw new UserError(this, "operator_toolbox.outlier.does_not_support_window", getParameterAsString(PARAMETER_METHOD));
		}

		double[] trainingData = new double[trainingBuffer.size()];
		for (int i = 0; i < trainingBuffer.size(); i++) {
			trainingData[i] = trainingBuffer.get(i);
		}
		outlierDetector.train(trainingData);

		int testSize = getParameterAsInt(PARAMETER_TEST_SIZE);

		for (int testWindowEnd = testingBuffer.size(); testWindowEnd - testSize >= 0; testWindowEnd -= testSize) {
			int testWindowStart = testWindowEnd - testSize;
			double[] testWindowData = new double[testSize];
			int arrayPosition = 0;
			// get the testing window
			for (int testWindowPosition = testWindowStart; testWindowPosition < testWindowEnd; testWindowPosition++) {
				testWindowData[arrayPosition] = testingBuffer.get(testWindowPosition);
				arrayPosition++;
			}
			// apply
			double[] scores = outlierDetector.apply(testWindowData);

			arrayPosition = 0;
			for (int testWindowPosition = testWindowStart; testWindowPosition < testWindowEnd; testWindowPosition++) {
				results.set(testWindowPosition, scores[arrayPosition]);
				arrayPosition++;
			}
		}
		return results;

	}

	/**
	 * @return an TSOutlierDetector as the user defines it in PARAMETER_METHOD
	 * @throws UndefinedParameterError if PARAMETER_METHOD is not correctly set.
	 */
	private AbstractTSOutlierDetector createOutlierDetector() throws UndefinedParameterError {
		String usedMethod = getParameterAsString(PARAMETER_METHOD);
		AbstractTSOutlierDetector outlierDetector;

		switch (usedMethod) {
			case ZSCORE:
				outlierDetector = new ZScoreOutlierDetector();
				break;
			case LINEAR_REGRESSION:
				outlierDetector = new LinearRegressionOutlierDetector(getParameterAsBoolean(LinearRegressionOutlierDetector.PARAMETER_NORMALIZE_REGRESSION_SCORES));
				break;
			case STD_DEV:
				outlierDetector = new StdOutlierDetector();
				break;
			default:
				throw new IllegalStateException("Unexpected value: " + usedMethod);
		}
		return outlierDetector;
	}

	/**
	 * Get a ScoreAggregation as the user defienrs in PARAMETER_AGGREGATION_METHODS
	 *
	 * @param takeAbsolutes Defines if the aggregation should run on the normal or absolute values
	 * @return a scoreaggregation method
	 * @throws OperatorException if a unknown aggregation method is provided in PARAMETER_AGGREGATION_METHODS
	 */
	private ScoreAggregation getScoreAggregation(boolean takeAbsolutes) throws OperatorException {
		String usedMethod = getParameterAsString(PARAMETER_AGGREGATION_METHODS);

		switch (usedMethod) {
			case "Average":
				return new AverageScoreAggregation(takeAbsolutes);
			case "Product":
				return new ProductScoreAggregation(takeAbsolutes);
			case "Maximum":
				return new MaxScoreAggregation(takeAbsolutes);
			default:
				throw new OperatorException("Cannot find aggregation method method " + usedMethod);
		}
	}


	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.addAll(attributeSelector.getParameterTypes());
		types.add(new ParameterTypeCategory(PARAMETER_METHOD, "the method to detect anomalies", AVAILABLE_METHODS, 0));
		types.add(new ParameterTypeCategory(PARAMETER_AGGREGATION_METHODS,
				"What method to use to create a single outlier score", AVAILABLE_AGGREGATIONS, 0));
		ParameterType trainingSize = new ParameterTypeInt(PARAMETER_TRAINING_SIZE, "The training window size", 1, Integer.MAX_VALUE, 12);
		trainingSize.registerDependencyCondition(new PortConnectedCondition(this, () -> refInput, true, false));
		types.add(trainingSize);

		types.add(new ParameterTypeInt(PARAMETER_TEST_SIZE, "The test window size", 1, Integer.MAX_VALUE, 1));
		ParameterTypeBoolean showAdvancedSettings = new ParameterTypeBoolean(PARAMETER_SHOW_ADVANCED_SETTINGS,"if set to true advanced settings are shown",false);
		types.add(showAdvancedSettings);

		ParameterTypeBoolean createIndividualScores = new ParameterTypeBoolean(PARAMETER_CREATE_INDIVIDUAL_SCORES,"if set to true individual scores are reported. Otherwise you only get the aggregated score",false);
		createIndividualScores.registerDependencyCondition(new BooleanParameterCondition(this,PARAMETER_SHOW_ADVANCED_SETTINGS,false,true));
		types.add(createIndividualScores);

		ParameterTypeBoolean useAbsoluteScores = new ParameterTypeBoolean(PARAMETER_USE_ABSOLUTES_FOR_AGGREGATION,"if set to true aggregations are performed on absolute scores.",true);
		useAbsoluteScores.registerDependencyCondition(new BooleanParameterCondition(this,PARAMETER_SHOW_ADVANCED_SETTINGS,false,true));
		types.add(useAbsoluteScores);

		List<ParameterType> regressionSettings = LinearRegressionOutlierDetector.getParameterTypes();
		for(ParameterType p : regressionSettings){
			p.registerDependencyCondition(new EqualStringCondition(this,PARAMETER_METHOD,true,LinearRegressionOutlierDetector.METHOD_NAME));
			p.registerDependencyCondition(new BooleanParameterCondition(this,PARAMETER_SHOW_ADVANCED_SETTINGS,false,true));
		}
		types.addAll(regressionSettings);
		return types;
	}

}
