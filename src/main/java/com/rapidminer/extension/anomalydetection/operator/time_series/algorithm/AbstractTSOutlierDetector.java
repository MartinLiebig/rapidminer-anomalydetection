package com.rapidminer.extension.anomalydetection.operator.time_series.algorithm;

import java.util.ArrayList;
import java.util.List;

import com.rapidminer.parameter.ParameterType;


public abstract class AbstractTSOutlierDetector {

	public AbstractTSOutlierDetector(){}


	/**
	 * Train the detector.
	 * @param x the array with the data to train
	 */
	public abstract void train(double[] x);

	/**
	 * apply the detector
	 * @param y the array with the data to apply it on.
	 */
	public abstract double[] apply(double[] y);
	/**
	 * Checks if the given TSOutlierDetector supports the capability
	 * @param capability The capability to test.
	 */
	public abstract boolean supportsCapability(TSOutlierCapability capability);

	public static List<ParameterType> getParameterTypes() {
		return new ArrayList<>();
	}


}
