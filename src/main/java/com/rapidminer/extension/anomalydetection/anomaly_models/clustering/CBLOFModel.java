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
	private CBLOFEvaluator evaluator;

	private double alpha;
	private double beta;

	private boolean useClusterWeights;
	/**
	 * Constructor for JSON serialization
	 */
	public CBLOFModel(){}

	public CBLOFModel(IOTable ioTable, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(ioTable, model,measure);

	}

	public NumericBuffer evaluate(ExampleSet testSet) throws OperatorException {
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, trainingHeader.getAttributes(),true);

		evaluator = new CBLOFEvaluator(alpha, beta,distanceMeasure,points,getClusterIds(testSet),centroids,clusterSize,useClusterWeights);
		double[] scores = evaluator.evaluate();
		NumericBuffer buffer = Buffers.realBuffer(scores.length);
		for(int i = 0; i < scores.length;++i){
			buffer.set(i,scores[i]);
		}
		return buffer;
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
