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
 * 	score = (value - median) / IQR
 * where IQR is the inter-quartile range (p75-p25).
 * This is a more robust version of zscore.
 *
 * @author MartinSchmitz
 */
public class QuartileScorer implements UnivariateScorer {

	double median = 0;
	double iqr = 0;

	public QuartileScorer() {
		super();
	}

	/**
	 * Calculates median and IQR and scores it
	 * @param c the column you want to calculate median and IQR on
	 * @param context context for parallel computation
	 */
	@Override
	public void train(Column c, Context context) {
		median = Statistics.compute(c,
				Statistics.Statistic.MEDIAN, context).getNumeric();
		double p25 = Statistics.compute(
				c,
				Statistics.Statistic.P25, context).getNumeric();
		double p75 = Statistics.compute(
				c,
				Statistics.Statistic.P75, context).getNumeric();
		iqr = Math.abs(p75-p25);
	}

	/**
	 * Calculates the anomaly score
	 * @param value the value you want to score
	 * @return Anomaly score as abs(median-value)/IQR
	 */
	public double score(double value) {
		return Math.abs(median - value) / iqr;
	}
}

