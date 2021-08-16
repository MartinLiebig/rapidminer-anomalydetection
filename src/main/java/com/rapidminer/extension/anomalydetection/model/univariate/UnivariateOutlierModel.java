/*
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.model.univariate;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import com.rapidminer.adaption.belt.ContextAdapter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Builders;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.TableBuilder;
import com.rapidminer.belt.table.TableViewCreator;
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.operator.univariate.HistogramBasedScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.QuartileScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.UnivariateScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.ZScorer;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.AverageScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.MaxScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ProductScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ScoreAggregation;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.PreprocessingModel;
import com.rapidminer.studio.internal.Resources;


/**
 * Univariate Anomaly Model which scores creates one anomaly model per attribute.
 * The individual scores are aggregated with one of the aggregation_methods;
 */
public class UnivariateOutlierModel extends PreprocessingModel {
	

	private final HashMap<String, UnivariateScorer> scorerMap = new HashMap<>();
	private List<ScoreAggregation> scoreAggregations;
	//private ExplainPredictionsIOObject explainPredictionsObject = null;
	
	private static final long serialVersionUID = -5464556244354792028L;

	private final String usedMethod;
	private final String usedAggregationMethod;
	private Boolean showScores;

	/**
	 * Create a model
	 * @param exampleSet training ES, used for headerExampleSet and such
	 * @param method the desiered anomaly detection method
	 * @param aggregationMethod the desired aggregation methods
	 * @param showScores if set to true one column will be created with the anomaly score for the given column.
	 */
	public UnivariateOutlierModel(ExampleSet exampleSet, String method, String aggregationMethod, Boolean showScores) {
		super(exampleSet);
		usedMethod = method;
		usedAggregationMethod = aggregationMethod;
		this.showScores = showScores;
	}

	/**
	 * Apply the model on the table. This copies the data.
	 */
	@Override
	public ExampleSet applyOnData(ExampleSet exampleSet) throws OperatorException {

		Table converted = BeltConverter.convert(exampleSet, getConcurrencyContext()).getTable();
		Table scored = this.scoreTable(converted,false, getContext());
		return BeltConverter.convert(new IOTable(scored),getConcurrencyContext());
	}
	/**
	 * Apply the model on the table. This copies the data.
	 */
	@Override
	public ExampleSet apply(ExampleSet exampleSet) throws OperatorException {
		return applyOnData(exampleSet);
	}

	@Override
	public Attributes getTargetAttributes(ExampleSet viewParent) {
		ExampleSet clonedExampleSet = (ExampleSet) viewParent.clone();
		try {
			clonedExampleSet = applyOnData(clonedExampleSet);
		} catch (OperatorException e) {
			this.log(e.getMessage());
		}
		return clonedExampleSet.getAttributes();
	}
	/**
	 * Get the score for a single attribute
	 */
	@Override
	public double getValue(Attribute targetAttribute, double value) {
		return scorerMap.get(targetAttribute.getName()).score(value);
	}

	/**
	 * Train the individual models
	 * @param table Table to build the model on
	 * @param trainingColumns List of column names you want to include in training
	 * @param context computation exception
	 * @throws OperatorException if the method is not found
	 *
	 */
	public void learnOnBelt(Table table, List<String> trainingColumns, Context context) throws OperatorException {

		for (String label : trainingColumns) {
			UnivariateScorer scorer;
			switch (usedMethod) {
				case "z-Score":
					scorer = new ZScorer();
					break;
				case "Histogram":
					scorer = new HistogramBasedScorer();
					break;
				case "Quartiles":
					scorer = new QuartileScorer();
					break;
				default:
					throw new OperatorException("Cannot find method " + usedMethod);
			}
			scorer.train(table.column(label), context);
			scorerMap.put(label, scorer);
		}

	}

	/**
	 * Scores a table with the trained model
	 * @param table the table to score
	 * @param createVisObject if set to true we will create the intermediate values. This allows you to call getExplainPredictionsIOObject.
	 */
	public Table scoreTable(Table table, boolean createVisObject, Context context) throws OperatorException {
		TableBuilder builder = Builders.newTableBuilder(table);
		scoreAggregations = new ArrayList<>(table.height());

		for (int rowId = 0; rowId < table.height(); rowId++) {
			//kv.add(new ArrayList<>());
			switch (usedAggregationMethod) {
				case "Average":
					scoreAggregations.add(new AverageScoreAggregation(false));
					break;
				case "Product":
					scoreAggregations.add(new ProductScoreAggregation(false));
					break;
				case "Maximum":
					scoreAggregations.add(new MaxScoreAggregation(false));
					break;
				default:
					throw new OperatorException("Cannot find aggregation method method " + usedMethod);
			}

		}

		for (String columnName : scorerMap.keySet()) {
			UnivariateScorer scorer = scorerMap.get(columnName);
			NumericBuffer buffer = table.transform(columnName)
					.applyNumericToReal(scorer :: score, context);
			for (int i = 0; i < buffer.size(); i++) {
				scoreAggregations.get(i).addValue(buffer.get(i));
			}
			if (createVisObject) {
//				for (int index = 0; index < buffer.size(); ++index) {
//					kv.get(index).add(new KeyAndValue(columnName, -1 * buffer.get(index), false, false));
//				}
			}
			if(showScores) {
				builder.add(columnName + "_score", buffer.toColumn())
						.addMetaData(columnName + "_score", ColumnRole.OUTLIER);
			}
		}
		NumericBuffer buffer = Buffers.realBuffer(table.height());
		for (int i = 0; i < table.height(); ++i) {
			buffer.set(i, scoreAggregations.get(i).getAggregate());
		}

//		explainPredictionsObject =  new ExplainPredictionsIOObject(
//					TableViewCreator.INSTANCE
//							.convertOnWriteView(new IOTable(table), false), kv);
		builder.add("outlier_score", buffer.toColumn()).addMetaData("outlier_score", ColumnRole.PREDICTION);
		return builder.build(context);
	}

//	/**
//	 * gets the explainpredictionsIoobject which is the colored result of the operator
//	 * @return the object
//	 * @throws OperatorException if the object was not created
//	 */
//	public ExplainPredictionsIOObject getExplainPredictionsIOObject() throws OperatorException {
//		if(explainPredictionsObject != null){
//			return explainPredictionsObject;
//		}
//		else{
//			throw new OperatorException("Did not calculate explainationobject and try to retrieve it.");
//		}
//	}

	private ConcurrencyContext getConcurrencyContext(){

		return Resources.getConcurrencyContext(this.getOperator());
	}

	private Context getContext(){

		return ContextAdapter.adapt(this.getConcurrencyContext());
	}
}
