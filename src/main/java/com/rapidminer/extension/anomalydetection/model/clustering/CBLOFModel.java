package com.rapidminer.extension.anomalydetection.model.clustering;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.CBLOFEvaluator;


/**
 * Deprecated because this does not use IOTablePredictionModel and is not using JSON.
 * Please use the model in anomaly_models package.
 * @author mschmitz
 */
@Deprecated
public class CBLOFModel extends ClusterBasedAnomalyDetectionModel {
	private CBLOFEvaluator evaluator;

	private double alpha;
	private double beta;

	private boolean useClusterWeights;
	private boolean largeCluster[];
	public CBLOFModel(ExampleSet exampleSet, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(exampleSet, model,measure);

	}
	public void train(ExampleSet trainSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(trainSet, getTrainingHeader().getAttributes(), true);
		largeCluster = CBLOFEvaluator.assignLargeClusters(clusterSize, alpha,
				beta, points.length);
	}

	public double[] evaluate(ExampleSet testSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet,getTrainingHeader().getAttributes(),true);

		evaluator = new CBLOFEvaluator(distanceMeasure,points,getClusterIds(testSet),centroids,clusterSize,useClusterWeights, largeCluster);
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
