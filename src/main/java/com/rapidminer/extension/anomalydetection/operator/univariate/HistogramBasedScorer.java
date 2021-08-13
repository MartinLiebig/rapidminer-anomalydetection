/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.column.Statistics;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.extension.anomalydetection.utility.Histogram;


/**
 * Use a histogram to calculate unvaried outlier score. The score is the inverse probability (frequency/size) of the
 * bin of the value. The number of bins is defined by Freedman-Diaconis rule See: https://en.wikipedia.org/wiki/Freedman%E2%80%93Diaconis_rule
 *
 * @author MartinSchmitz
 */
public class HistogramBasedScorer implements UnivariateScorer {
	Histogram histogram;
	int size;

	/**
	 * Creates the histogram.
	 *
	 * @param c
	 * 		column to generate the histogram for
	 * @param context
	 * 		computation context.
	 */
	@Override
	public void train(Column c, Context context) {
		size = c.size();
		double p25 = Statistics.compute(c,
				Statistics.Statistic.P25, context).getNumeric();
		double p75 = Statistics.compute(c,
				Statistics.Statistic.P75, context).getNumeric();
		double min = Statistics.compute(c,
				Statistics.Statistic.MIN, context).getNumeric();
		double max = Statistics.compute(c,
				Statistics.Statistic.MAX, context).getNumeric();
		// this is the Freedman-Diaconis rule
		double IQR = Math.abs(p75 - p25);
		double binWidth = 2 * IQR / Math.pow(size, 1.0 / 3);
		int bins = (int) Math.round(Math.abs(max - min) / binWidth);

		histogram = new Histogram(min, max, bins);

		double[] data = new double[c.size()];
		c.fill(data, 0);
		for (double d : data) {
			histogram.add(d);
		}
	}


	/**
	 * Calculates the anomaly score. The score is defined as: 1/probability. Where probability is the frequency in the
	 * given bin +1 / training sample. The +1 is used to avoid division by 0.
	 */
	@Override
	public double score(double value) {
		double probability = (histogram.getFrequency(value) + 1) / size;
		return 1 / probability;
	}
}
