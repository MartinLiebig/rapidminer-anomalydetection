/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rapidminer.extension.anomalydetection.operator.univariate.HistogramBasedScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.QuartileScorer;
import com.rapidminer.extension.anomalydetection.operator.univariate.ZScorer;


/**
 * Different aggregation used in UnivariateOutlierModel to aggregate one anomaly score out of many individual column
 * scores
 *
 * @author MartinSchmitz
 */
@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type",
		defaultImpl = AverageScoreAggregation.class
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = AverageScoreAggregation.class, name = "average"),
		@JsonSubTypes.Type(value = MaxScoreAggregation.class, name = "max"),
		@JsonSubTypes.Type(value = ProductScoreAggregation.class, name = "product")
})
public abstract class ScoreAggregation {
	protected boolean takeAbsolutes;
	public ScoreAggregation(){}
	public ScoreAggregation(boolean takeAbsolutes){
		this.takeAbsolutes = takeAbsolutes;
	}
	/**
	 * Add a value to the calculation. The final calculation may happen here,
	 * or when you when you retrieve the aggregate
	 * @param value
	 */
	public abstract void addValue(double value);

	/**
	 * calculates the aggregate of the values provided by addValue()
	 * @return aggregate of all values provided up front.
	 */
	public abstract double getAggregate();
}
