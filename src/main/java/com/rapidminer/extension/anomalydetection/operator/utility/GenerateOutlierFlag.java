package com.rapidminer.extension.anomalydetection.operator.utility;

import java.util.List;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.ColumnType;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.belt.util.Order;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.extension.anomalydetection.operator.utility.flag_generator.ThresholdFlagModel;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.GeneralModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorDescription;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.learner.functions.FunctionFittingModel;
import com.rapidminer.operator.ports.IncompatibleMDClassException;
import com.rapidminer.operator.ports.InputPort;
import com.rapidminer.operator.ports.OutputPort;
import com.rapidminer.operator.ports.metadata.GenerateTableModelTransformationRule;
import com.rapidminer.operator.ports.metadata.MDTransformationRule;
import com.rapidminer.operator.ports.metadata.table.ColumnInfoBuilder;
import com.rapidminer.operator.ports.metadata.table.TableMetaData;
import com.rapidminer.operator.ports.metadata.table.TableMetaDataBuilder;
import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeAttribute;
import com.rapidminer.parameter.ParameterTypeBoolean;
import com.rapidminer.parameter.ParameterTypeCategory;
import com.rapidminer.parameter.ParameterTypeDouble;
import com.rapidminer.parameter.conditions.BooleanParameterCondition;
import com.rapidminer.parameter.conditions.EqualStringCondition;
import com.rapidminer.studio.internal.Resources;
import com.rapidminer.tools.Ontology;
import com.rapidminer.tools.belt.BeltTools;


public class GenerateOutlierFlag extends Operator {
	InputPort exaInput = getInputPorts().createPort("example set", IOTable.class);

	OutputPort exaOutput = getOutputPorts().createPort("example set");
	OutputPort oriOutput = getOutputPorts().createPort("original");
	OutputPort modOutput = getOutputPorts().createPort("model");

	public static final String PARAMETER_METHOD = "method";
	public static final String PARAMETER_DEFINE_SCORE_COLUMN = "define_score_column";
	public static final String PARAMETER_SCORE_COLUMN = "score_column";

	public static final String METHOD_CONTAMINATION = "contamination";
	public static final String METHOD_ZSCORE = "z-score";
	public static final String METHOD_MANUAL = "manual";
	public static final String[] PARAMETER_AVAILABLE_METHODS = new String[]{METHOD_CONTAMINATION, METHOD_MANUAL, METHOD_ZSCORE};

	public static final String PARAMETER_MANUAL_THRESHOLD = "manual_threshold";
	public static final String PARAMETER_ZSCORE_THRESHOLD = "zscore_threshold";
	public static final String PARAMETER_CONTAMINATION_THRESHOLD = "contamination_threshold";

	public GenerateOutlierFlag(OperatorDescription description) {
		super(description);
		getTransformer().addPassThroughRule(exaInput,oriOutput);
//		getTransformer().addRule(
//				new GenerateTableModelTransformationRule(exaInput, modOutput, ThresholdFlagModel.class,
//						GeneralModel.ModelKind.POSTPROCESSING));
		getTransformer().addGenerationRule(modOutput,ThresholdFlagModel.class);
		getTransformer().addRule(() -> {
			try {
				TableMetaData tmd = exaInput.getMetaData(TableMetaData.class);
				if(tmd!=null) {
					TableMetaDataBuilder builder = new TableMetaDataBuilder(tmd);
					ColumnInfoBuilder columnInfoBuilder =
							new ColumnInfoBuilder(ColumnType.NOMINAL)
									.setBooleanDictionaryValues(AnomalyUtilities.ANOMALY_NAME, AnomalyUtilities.NO_ANOMALY_NAME);
					builder.add(AnomalyUtilities.ANOMALY_FLAG_NAME, columnInfoBuilder.build()).addColumnMetaData(AnomalyUtilities.ANOMALY_FLAG_NAME, ColumnRole.PREDICTION);
					exaOutput.deliverMD(builder.build());
				}
			} catch (IncompatibleMDClassException e) {
				e.printStackTrace();
			}
		});
	}

	public void doWork() throws OperatorException {
		IOTable inputData = exaInput.getData(IOTable.class);

		double threshold = 1;
		ConcurrencyContext concurrencyContext = Resources.getConcurrencyContext(this);
		Context beltContext = ContextAdapter.adapt(concurrencyContext);

		String scoreColumnName;
		if(getParameterAsBoolean(PARAMETER_DEFINE_SCORE_COLUMN)){
			scoreColumnName = getParameterAsString(PARAMETER_SCORE_COLUMN);
		}
		else{
			try {
				scoreColumnName = inputData.getTable().select().withMetaData(ColumnRole.SCORE).labels().get(0);
			}catch (Exception e){
				throw new UserError(this,"anomaly_detection.outlier_flag.cant_find_score");
			}
		}

		Column scoreColumn = inputData.getTable().column(scoreColumnName);
		if(!scoreColumn.type().equals(ColumnType.REAL)){
			throw new UserError(this,"anomaly_detection.outlier_flag.score_not_real",scoreColumnName,scoreColumn.type().id().toString());
		}
		if(BeltTools.containsMissingValues(scoreColumn)){
			throw new UserError(this, 139,scoreColumnName);
		}
		switch (getParameterAsString(PARAMETER_METHOD)) {
			case METHOD_MANUAL:
				threshold = getParameterAsDouble(PARAMETER_MANUAL_THRESHOLD);
				break;
			case METHOD_ZSCORE:
				double mean = Statistics.compute(scoreColumn, Statistics.Statistic.MEAN, beltContext).getNumeric();
				double sd = Statistics.compute(scoreColumn, Statistics.Statistic.SD, beltContext).getNumeric();
				threshold = mean+getParameterAsDouble(PARAMETER_ZSCORE_THRESHOLD)*sd;
				break;
			case METHOD_CONTAMINATION:

				Column sorted = scoreColumn.rows(scoreColumn.sort(Order.ASCENDING), true);
				NumericReader reader = Readers.numericReader(sorted);
				threshold = AnomalyUtilities.computePercentile(reader, sorted.size(), 1-getParameterAsDouble(PARAMETER_CONTAMINATION_THRESHOLD));


		}
		ThresholdFlagModel model = new ThresholdFlagModel(inputData, threshold,scoreColumnName);

		oriOutput.deliver(inputData);
		exaOutput.deliver(model.apply(inputData, this));
		modOutput.deliver(model);


	}

	@Override
	public List<ParameterType> getParameterTypes() {
		List<ParameterType> types = super.getParameterTypes();
		types.add(new ParameterTypeCategory(PARAMETER_METHOD, "The method for flag generation", PARAMETER_AVAILABLE_METHODS, 0));
		types.add(new ParameterTypeBoolean(PARAMETER_DEFINE_SCORE_COLUMN,"if set to true you can manually determine the score column",false));
		ParameterType scoreColumn = new ParameterTypeAttribute(PARAMETER_SCORE_COLUMN,"The column used for score calculation",exaInput, Ontology.REAL);
		scoreColumn.registerDependencyCondition(new BooleanParameterCondition(this,PARAMETER_DEFINE_SCORE_COLUMN,true,true));
		types.add(scoreColumn);

		ParameterType contaminationTreshold = new ParameterTypeDouble(PARAMETER_CONTAMINATION_THRESHOLD, "threshold above which a value is considerd an anomaly.", 0, 1, 0.05);
		contaminationTreshold.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_METHOD, true, METHOD_CONTAMINATION));
		types.add(contaminationTreshold);

		ParameterType manualTreshold = new ParameterTypeDouble(PARAMETER_MANUAL_THRESHOLD, "threshold above which a value is considerd an anomaly.", -1 * Double.MAX_VALUE, Double.MAX_VALUE, 1);
		manualTreshold.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_METHOD, true, METHOD_MANUAL));
		types.add(manualTreshold);

		ParameterType zscoreThreshold = new ParameterTypeDouble(PARAMETER_ZSCORE_THRESHOLD, "threshold above which a value is considerd an anomaly.", -1 * Double.MAX_VALUE, Double.MAX_VALUE, 3);
		zscoreThreshold.registerDependencyCondition(new EqualStringCondition(this, PARAMETER_METHOD, true, METHOD_ZSCORE));
		types.add(zscoreThreshold);

		return types;
	}
}
