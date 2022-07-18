package com.rapidminer.extension.anomalydetection.model.clustering;

import static de.dfki.madm.anomalydetection.evaluator.cluster_based.LDCOFEvaluator.assignLargeClusters;

import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.CBLOFEvaluator;
import de.dfki.madm.anomalydetection.evaluator.cluster_based.LDCOFEvaluator;

/**
 * Deprecated because this does not use IOTablePredictionModel and is not using JSON.
 * Please use the model in anomaly_models package.
 * @author mschmitz
 */
@Deprecated
public class LDCOFModel extends ClusterBasedAnomalyDetectionModel {
	private double alpha;
	private double beta;
	private double gamma;
	private boolean useGamma;
	private boolean[] largeCluster;

	public LDCOFModel(ExampleSet exampleSet, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(exampleSet, model, measure);
	}

	public void train(ExampleSet testSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, getTrainingHeader().getAttributes(), true);
		if(useGamma){
			largeCluster = assignLargeClusters(clusterSize, gamma * points.length
					/ centroids.length);
		}else{
			largeCluster = CBLOFEvaluator.assignLargeClusters(clusterSize, alpha,
					beta, points.length);
		}
	}

	@Override
	public double[] evaluate(ExampleSet testSet) throws OperatorException {

		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, getTrainingHeader().getAttributes(), true);
		LDCOFEvaluator evaluator;
		if(useGamma)
			evaluator = new LDCOFEvaluator(gamma, distanceMeasure, points, getClusterIds(testSet), centroids, clusterSize,largeCluster);
		else {
		 evaluator =new LDCOFEvaluator(alpha, beta, distanceMeasure, points, getClusterIds(testSet), centroids, clusterSize,largeCluster);
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
