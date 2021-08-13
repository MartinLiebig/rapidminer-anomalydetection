package com.rapidminer.extension.anomalydetection.operator.time_series.algorithm;

import java.util.ArrayList;
import java.util.List;

import org.apache.commons.math3.stat.regression.RegressionResults;
import org.apache.commons.math3.stat.regression.SimpleRegression;

import com.rapidminer.parameter.ParameterType;
import com.rapidminer.parameter.ParameterTypeBoolean;


public class LinearRegressionOutlierDetector extends AbstractTSOutlierDetector {
	public static final String METHOD_NAME = "Linear Regression";

	private SimpleRegression simpleRegression;
	private RegressionResults regressionResults;
	private int trainSize;
	private boolean normalizeScores;

	public final static String PARAMETER_NORMALIZE_REGRESSION_SCORES = "normalize_regression_scores";

	public LinearRegressionOutlierDetector(boolean normalizeScores) {
		this.normalizeScores = normalizeScores;

	}

	@Override
	public void train(double[] x) {
		simpleRegression = new SimpleRegression(true);
		trainSize = x.length;
		for (int i = 0; i < x.length; ++i) {
			simpleRegression.addData(i, x[i]);
		}
		regressionResults = simpleRegression.regress();
	}

	@Override
	public double[] apply(double[] y) {
		double[] scores = new double[y.length];
		for (int i = 0; i < y.length; ++i) {
			double f = simpleRegression.predict(i + (double) trainSize);

			scores[i] = (f - y[i]);
			scores[i] = normalizeScores ? scores[i] / f : scores[i];

		}
		return scores;
	}

	@Override
	public boolean supportsCapability(TSOutlierCapability capability) {
		switch (capability) {
			case SUPPORTS_SLIDING_WINDOW:
				return true;
			case SUPPORTS_TRAINING:
				return false;
			default:
				return false;
		}
	}

	public static List<ParameterType> getParameterTypes() {
		List<ParameterType> types = new ArrayList<>();
		types.add(new ParameterTypeBoolean(PARAMETER_NORMALIZE_REGRESSION_SCORES, "if set to true scores are relative deviations, otherwise you get the raw delta.", true));
		return types;
	}
}
