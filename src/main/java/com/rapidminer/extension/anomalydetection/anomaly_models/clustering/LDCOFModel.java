package com.rapidminer.extension.anomalydetection.anomaly_models.clustering;

import static de.dfki.madm.anomalydetection.evaluator.cluster_based.LDCOFEvaluator.assignLargeClusters;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.CBLOFEvaluator;
import de.dfki.madm.anomalydetection.evaluator.cluster_based.LDCOFEvaluator;


public class LDCOFModel extends ClusterBasedAnomalyDetectionModel {
	boolean[] largeCluster;
	double[] averageDistances;

	private LDCOFModel() {
		super();
	}

	public LDCOFModel(IOTable ioTable, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(ioTable, model, measure);

	}

	public void train(ExampleSet trainSet, double alpha, double beta) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(trainSet, trainingHeader.getAttributes(), true);
		largeCluster = CBLOFEvaluator.assignLargeClusters(clusterSize, alpha,
				beta, points.length);
		averageDistances = LDCOFEvaluator.calculateAverageDistancePerCluster(
				distanceMeasure, points, centroids,
				getClusterIds(trainSet), clusterSize, largeCluster);

	}

	public void train(ExampleSet trainSet, double gamma) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(trainSet, trainingHeader.getAttributes(), true);
		largeCluster = assignLargeClusters(clusterSize, gamma * points.length
				/ centroids.length);
		averageDistances = LDCOFEvaluator.calculateAverageDistancePerCluster(
				distanceMeasure, points, centroids,
				getClusterIds(trainSet), clusterSize, largeCluster);
	}

	@Override
	public NumericBuffer evaluate(ExampleSet testSet) throws OperatorException {

		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, trainingHeader.getAttributes(), true);
		LDCOFEvaluator evaluator;

		evaluator = new LDCOFEvaluator(distanceMeasure, points, getClusterIds(testSet), centroids, clusterSize, largeCluster, averageDistances);

		double[] scores = evaluator.evaluate();
		NumericBuffer buffer = Buffers.realBuffer(scores.length);
		for (int i = 0; i < scores.length; ++i) {
			buffer.set(i, scores[i]);
		}
		return buffer;
	}

}
