package com.rapidminer.extension.anomalydetection.model.clustering;

import java.util.Arrays;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.extension.anomalydetection.model.AnomalyDetectionModel;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

/**
 * Deprecated because this does not use IOTablePredictionModel and is not using JSON.
 * Please use the model in anomaly_models package.
 * @author mschmitz
 */
@Deprecated
public abstract class ClusterBasedAnomalyDetectionModel extends AnomalyDetectionModel {
	protected DistanceMeasure distanceMeasure;
	protected ClusterModel clusterModel;
	protected double[][] centroids;
	protected int[] clusterSize;
	boolean trained = false;

	private ClusterBasedAnomalyDetectionModel(ExampleSet exampleSet) {
		super(exampleSet);
	}


	public ClusterBasedAnomalyDetectionModel(ExampleSet exampleSet, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(exampleSet);
		clusterModel = model;
		distanceMeasure = measure;
		centroids = getCentroids(exampleSet);
		clusterSize = getClusterSize(exampleSet);

	}

	@Override
	public abstract double[] evaluate(ExampleSet testSet) throws OperatorException;


	/**
	 * calculates the centroids of a given cluster on the trainingset.
	 * @param trainingSet
	 * @return
	 */
	protected double[][] getCentroids(ExampleSet trainingSet) throws OperatorException {
		int nAtts = trainingSet.getAttributes().size();
		double[][] centroids = new double[clusterModel.getNumberOfClusters()][nAtts];
//		if(clusterModel instanceof  CentroidClusterModel) {
//			CentroidClusterModel centroidClusterModel = (CentroidClusterModel) clusterModel;
//			int i = 0;
//			for (Centroid c : centroidClusterModel.getCentroids()) {
//				centroids[i] = c.getCentroid();
//				i++;
//			}
//		}else{
			ExampleSet appliedSet = clusterModel.apply(trainingSet);
			Attribute clusterId = appliedSet.getAttributes().getCluster();
			NominalMapping originalMapping = getTrainingHeader().getAttributes().getCluster().getMapping();
			int[] clusterCount = new int[clusterModel.getNumberOfClusters()];

			Arrays.fill(clusterCount,0);
			for(Example e : appliedSet){
				String clusterName = e.getNominalValue(clusterId);
				int clusterIndex = originalMapping.getIndex(clusterName);
				int attributeIndex = 0;
				if(clusterIndex == -1){
					throw new OperatorException("Cannot find "+clusterName+" in your training set");
				}

				for(Attribute a : clusterModel.getTrainingHeader().getAttributes()){
					centroids[clusterIndex][attributeIndex]+= e.getValue(e.getAttributes().get(a.getName()));
					attributeIndex++;
				}
				clusterCount[clusterIndex]=clusterCount[clusterIndex]+1;
			}
			for(int clusterindex = 0; clusterindex < clusterModel.getNumberOfClusters(); ++clusterindex){
				for(int attributeIndex = 0; attributeIndex<nAtts;++attributeIndex){
					centroids[clusterindex][attributeIndex]/=clusterCount[clusterindex];
				}

			}
		//}
		return  centroids;
	}

	/**
	 * calculates the size of each cluster
	 * @param trainingSet the example set to count
	 * @return an array with sizes of each cluster
	 * @throws OperatorException
	 */
	protected int[] getClusterSize(ExampleSet trainingSet) throws OperatorException {
		int[] clusterSizes = new int[clusterModel.getNumberOfClusters()];
		Arrays.fill(clusterSizes,0);
		ExampleSet appliedSet = clusterModel.apply(trainingSet);

		Attribute clusterId = appliedSet.getAttributes().getCluster();
		NominalMapping originalMapping = getTrainingHeader().getAttributes().getCluster().getMapping();
		for(Example e : appliedSet){
			String clusterName = e.getNominalValue(clusterId);
			int index = originalMapping.getIndex(clusterName);
			clusterSizes[index] = clusterSizes[index]+1;
		}
		return clusterSizes;
	}

	protected int[] getClusterIds(ExampleSet testSet) throws OperatorException {
		int[] clusterAssignments = new int[testSet.size()];
		ExampleSet appliedSet = clusterModel.apply(testSet);

		Attribute clusterId = appliedSet.getAttributes().getCluster();
		NominalMapping originalMapping = getTrainingHeader().getAttributes().getCluster().getMapping();
		int i = 0;
		for(Example e : appliedSet){
			String clusterName = e.getNominalValue(clusterId);
			int index = originalMapping.getIndex(clusterName);
			clusterAssignments[i] = index;
			i++;
		}
		return clusterAssignments;
	}




}
