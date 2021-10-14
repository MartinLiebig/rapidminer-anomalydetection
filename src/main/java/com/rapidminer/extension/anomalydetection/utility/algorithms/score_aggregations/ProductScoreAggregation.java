/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations;

/**
 * Calculates the product divided by number of scores for the given values in add value. For stability we use sum of
 * logs instead of product.
 * @author MartinSchmitz
 */

public class ProductScoreAggregation extends ScoreAggregation {
	double aggregate = 0;
	int count;

	public ProductScoreAggregation(boolean takeAbsolutes) {
		super(takeAbsolutes);
	}

	@Override
	public void addValue(double value) {
		if(!Double.isNaN(value)) {
			aggregate += Math.log(value);
			count++;
		}
	}

	@Override
	public double getAggregate() {
		return Math.exp(aggregate) / count;
	}
}
