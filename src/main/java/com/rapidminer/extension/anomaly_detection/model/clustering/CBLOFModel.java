package com.rapidminer.extension.anomaly_detection.model.clustering;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomaly_detection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.CBLOFEvaluator;


public class CBLOFModel extends ClusterBasedAnomalyDetectionModel {
	private CBLOFEvaluator evaluator;

	private double alpha;
	private double beta;

	private boolean useClusterWeights;

	public CBLOFModel(ExampleSet exampleSet, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(exampleSet, model,measure);

	}

	public double[] evaluate(ExampleSet testSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet,getTrainingHeader().getAttributes(),true);

		evaluator = new CBLOFEvaluator(alpha, beta,distanceMeasure,points,getClusterIds(testSet),centroids,clusterSize,useClusterWeights);
		double[] scores = evaluator.evaluate();

		return scores;
	}

	public void setAlpha(double alpha) {
		this.alpha = alpha;
	}

	public void setBeta(double beta) {
		this.beta = beta;
	}

	public void setUseClusterWeights(boolean useClusterWeights) {
		this.useClusterWeights = useClusterWeights;
	}



}
