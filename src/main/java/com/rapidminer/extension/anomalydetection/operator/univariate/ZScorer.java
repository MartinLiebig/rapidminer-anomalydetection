/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.execution.Context;


/**
 * Calculates an anomaly score as
 * 	score = (value-mean)/std_dev
 * which is the distance to the mean in number of standard deviations.
 *
 * @author MartinSchmitz
 */
public class ZScorer implements UnivariateScorer {

	double mean = 0;
	double std_dev = 0;

	public ZScorer() {
		super();
	}

	/**
	 * Calculates mean and std_dev
	 * @param c the column you want to extract mean and std_dev on
	 * @param context computation context for parallel execution
	 */
	@Override
	public void train(Column c, Context context) {
		mean = Statistics.compute(c,
				Statistics.Statistic.MEAN, context).getNumeric();
		std_dev = Statistics.compute(
				c,
				Statistics.Statistic.SD, context).getNumeric();
	}

	/**
	 * calculates the anomaly score as
	 * abs(mean-value)/std_dev
	 * @param value the value you want to get the anomaly score for
	 * @return the anomaly score.
	 */
	public double score(double value) {
		return Math.abs(mean - value) / std_dev;
	}
}

