/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations;

/**
 * Different aggregation used in UnivariateOutlierModel to aggregate one anomaly score out of many individual column
 * scores
 *
 * @author MartinSchmitz
 */
public abstract class ScoreAggregation {
	protected boolean takeAbsolutes;
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
