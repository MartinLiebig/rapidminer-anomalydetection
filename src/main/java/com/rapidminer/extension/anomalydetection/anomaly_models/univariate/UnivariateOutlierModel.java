/*
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.anomaly_models.univariate;

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
import com.rapidminer.belt.util.ColumnRole;
import com.rapidminer.core.concurrency.ConcurrencyContext;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.operator.univariate.HistogramBasedScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.QuartileScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.UnivariateScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.ZScorer;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.AverageScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.MaxScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ProductScoreAggregation;
import com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations.ScoreAggregation;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.preprocessing.IOTablePreprocessingModel;
import com.rapidminer.studio.internal.Resources;


/**
 * Univariate Anomaly Model which scores creates one anomaly model per attribute.
 * The individual scores are aggregated with one of the aggregation_methods;
 */
public class UnivariateOutlierModel extends IOTablePreprocessingModel {


	private final HashMap<String, UnivariateScorer> scorerMap = new HashMap<>();
	private List<ScoreAggregation> scoreAggregations;
	//private ExplainPredictionsIOObject explainPredictionsObject = null;

	private static final long serialVersionUID = -5464556244354792028L;

	private final String usedMethod;
	private final String usedAggregationMethod;
	private Boolean showScores;

	public UnivariateOutlierModel() {
		super(null);
		usedMethod = "";
		usedAggregationMethod = "";
	}


	/**
	 * Create a model
	 *
	 * @param table        		training table, used for headerExampleSet and such
	 * @param method            the desiered anomaly detection method
	 * @param aggregationMethod the desired aggregation methods
	 * @param showScores        if set to true one column will be created with the anomaly score for the given column.
	 */
	public UnivariateOutlierModel(IOTable table, String method, String aggregationMethod, Boolean showScores) {
		super(table);
		usedMethod = method;
		usedAggregationMethod = aggregationMethod;
		this.showScores = showScores;
	}

	@Override
	public void applyOnData(Table adjusted, TableBuilder builder, Operator operator) throws OperatorException {
		Context context = ContextAdapter.adapt(Resources.getConcurrencyContext(operator));
		scoreAggregations = new ArrayList<>(adjusted.height());

		for (int rowId = 0; rowId < adjusted.height(); rowId++) {
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
			NumericBuffer buffer = adjusted.transform(columnName)
					.applyNumericToReal(scorer::score, context);
			for (int i = 0; i < buffer.size(); i++) {
				scoreAggregations.get(i).addValue(buffer.get(i));
			}
			if (showScores) {
				builder.add(Attributes.PREDICTION_NAME + "(" + columnName + ")", buffer.toColumn())
						.addMetaData(Attributes.PREDICTION_NAME + "(" + columnName + ")", ColumnRole.PREDICTION);
			}
		}
		NumericBuffer buffer = Buffers.realBuffer(adjusted.height());
		for (int i = 0; i < adjusted.height(); ++i) {
			buffer.set(i, scoreAggregations.get(i).getAggregate());
		}
		builder.add(Attributes.PREDICTION_NAME, buffer.toColumn()).addMetaData(Attributes.PREDICTION_NAME, ColumnRole.PREDICTION);

	}

	@Override
	protected boolean needsRemapping() {
		return false;
	}

	/**
	 * Train the individual models
	 *
	 * @param table           Table to build the model on
	 * @param trainingColumns List of column names you want to include in training
	 * @param context         computation exception
	 * @throws OperatorException if the method is not found
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



}
