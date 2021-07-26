package com.rapidminer.extension.anomaly_detection.model.clustering;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomaly_detection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.LDCOFEvaluator;


public class LDCOFModel extends ClusterBasedAnomalyDetectionModel {
	private double alpha;
	private double beta;


	private double gamma;
	private boolean useGamma;

	public LDCOFModel(ExampleSet exampleSet, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(exampleSet, model, measure);
	}

	@Override
	public double[] evaluate(ExampleSet testSet) throws OperatorException {

		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, getTrainingHeader().getAttributes(), true);
		LDCOFEvaluator evaluator;
		if(useGamma)
			evaluator = new LDCOFEvaluator(gamma, distanceMeasure, points, getClusterIds(testSet), centroids, clusterSize);
		else {
		 evaluator =new LDCOFEvaluator(alpha, beta, distanceMeasure, points, getClusterIds(testSet), centroids, clusterSize);
		}
		 double[] scores = evaluator.evaluate();
		return scores;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public void setGamma(double gamma) {
		this.gamma = gamma;
	}

	public void setUseGamma(boolean useGamma) {
		this.useGamma = useGamma;
	}
}
