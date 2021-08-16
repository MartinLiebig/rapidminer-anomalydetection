/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations;


/**
 * Calculates the average of the values provided with addValue
 *
 * @author MartinSchmitz
 */
public class AverageScoreAggregation extends ScoreAggregation {
	double aggregation =0;
	int count = 0;

	public AverageScoreAggregation(boolean takeAbsolutes) {
		super(takeAbsolutes);
	}

	@Override
	public void addValue(double value) {
		if(!Double.isNaN(value)) {
			aggregation += value;
			count++;
		}
	}

	@Override
	public double getAggregate() {
		return aggregation/count;
	}
}
