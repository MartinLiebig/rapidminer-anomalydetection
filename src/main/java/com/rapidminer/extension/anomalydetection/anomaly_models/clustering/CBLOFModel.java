package com.rapidminer.extension.anomalydetection.anomaly_models.clustering;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.CBLOFEvaluator;


public class CBLOFModel extends ClusterBasedAnomalyDetectionModel {


	private boolean useClusterWeights;
	private boolean[] largeCluster;

	public CBLOFModel(){
		super();
	}

	public CBLOFModel(IOTable ioTable, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(ioTable, model,measure);

	}

	public void train(ExampleSet trainSet, double alpha, double beta) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(trainSet,trainingHeader.getAttributes(),true);
		largeCluster = CBLOFEvaluator.assignLargeClusters(clusterSize, alpha,
				beta, points.length);
	}

	public NumericBuffer evaluate(ExampleSet testSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet,trainingHeader.getAttributes(),true);

		CBLOFEvaluator evaluator = new CBLOFEvaluator(distanceMeasure,points,getClusterIds(testSet),centroids,clusterSize,useClusterWeights,largeCluster);
		double[] scores = evaluator.evaluate();
		NumericBuffer buffer = Buffers.realBuffer(scores.length);
		for(int i = 0; i < scores.length;++i){
			buffer.set(i,scores[i]);
		}
		return buffer;
	}


	public void setUseClusterWeights(boolean useClusterWeights) {
		this.useClusterWeights = useClusterWeights;
	}



}
