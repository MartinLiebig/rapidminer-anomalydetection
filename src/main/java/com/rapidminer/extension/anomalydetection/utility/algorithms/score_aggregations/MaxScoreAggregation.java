/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility.algorithms.score_aggregations;

/**
 * calculates the maximum of the values given in addValue.
 *
 * @author MartinSchmitz
 */
public class MaxScoreAggregation extends ScoreAggregation {
	double aggregate = Double.NaN;

	public MaxScoreAggregation(){}
	public MaxScoreAggregation(boolean takeAbsolutes) {
		super(takeAbsolutes);
	}


	@Override
	public void addValue(double value) {
		if(takeAbsolutes)
			value = Math.abs(value);
		if (Double.isNaN(aggregate)) {
			aggregate = value;
		} else {
			aggregate = Math.max(aggregate, value);
		}

	}

	@Override
	public double getAggregate() {
		return aggregate;
	}
}
