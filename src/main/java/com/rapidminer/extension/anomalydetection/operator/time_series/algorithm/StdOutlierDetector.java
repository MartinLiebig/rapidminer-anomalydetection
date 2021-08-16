package com.rapidminer.extension.anomalydetection.operator.time_series.algorithm;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;


public class StdOutlierDetector extends AbstractTSOutlierDetector {
	public final static String METHOD_NAME = "Standard Deviation";

	private double stdDev = 0;
	private StandardDeviation sd = new StandardDeviation();

	@Override
	public void train(double[] x) {
		stdDev = sd.evaluate(x);
	}

	@Override
	public double[] apply(double[] y) {
		double score = sd.evaluate(y)/ stdDev;
		double[] scores = new double[y.length];
		Arrays.fill(scores, score);
		return scores;
	}
	@Override
	public boolean supportsCapability(TSOutlierCapability capability) {
		switch (capability) {
			case SUPPORTS_SLIDING_WINDOW:
				return true;
			case SUPPORTS_TRAINING:
				return true;
			default:
				return false;
		}
	}
}
