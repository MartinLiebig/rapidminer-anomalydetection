package com.rapidminer.extension.anomalydetection.operator.time_series.algorithm;

import java.util.Arrays;

import org.apache.commons.math3.stat.descriptive.moment.StandardDeviation;


public class ZScoreOutlierDetector extends AbstractTSOutlierDetector {
	public static final  String METHOD_NAME = "z-Score";
	private double mean = 0;
	private double stdDev = 0;

	@Override
	public void train(double[] x) {
		mean = Arrays.stream(x).sum() / x.length;
		StandardDeviation sd = new StandardDeviation();
		stdDev = sd.evaluate(x);
	}

	@Override
	public double[] apply(double[] y) {
		return Arrays.stream(y).map(yi -> (yi - mean) / stdDev).toArray();
	}

	protected static String setMethodName() {
		return "Z-Score";
	}

	@Override
	public boolean supportsCapability(TSOutlierCapability capability) {
		switch (capability) {
			case SUPPORTS_SLIDING_WINDOW:
			case SUPPORTS_TRAINING:
				return true;
			default:
				return false;
		}
	}
}
