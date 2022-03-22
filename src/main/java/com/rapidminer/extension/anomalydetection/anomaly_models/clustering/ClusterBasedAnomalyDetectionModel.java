package com.rapidminer.extension.anomalydetection.anomaly_models.clustering;

import java.util.Arrays;
import java.util.Map;

import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.execution.SequentialContext;
import com.rapidminer.belt.table.BeltConverter;
import com.rapidminer.belt.table.Table;
import com.rapidminer.belt.table.Tables;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.set.HeaderExampleSet;
import com.rapidminer.example.table.NominalMapping;
import com.rapidminer.extension.anomalydetection.anomaly_models.IOTableAnomalyModel;
import com.rapidminer.extension.anomalydetection.model.AnomalyDetectionModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.operator.learner.IOTablePredictionModel;
import com.rapidminer.studio.concurrency.internal.SequentialConcurrencyContext;
import com.rapidminer.tools.belt.BeltTools;
import com.rapidminer.tools.math.similarity.DistanceMeasure;


public abstract class ClusterBasedAnomalyDetectionModel extends IOTableAnomalyModel {
	protected DistanceMeasure distanceMeasure;
	protected ClusterModel clusterModel;
	protected double[][] centroids;
	protected int[] clusterSize;

	protected HeaderExampleSet trainingHeader;

	public ClusterBasedAnomalyDetectionModel(){}

	public ClusterBasedAnomalyDetectionModel(IOTable ioTable, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(ioTable, Tables.ColumnSetRequirement.EQUAL, Tables.TypeRequirement.ALLOW_INT_FOR_REAL);
		clusterModel = model;
		distanceMeasure = measure;
		SequentialConcurrencyContext context = new SequentialConcurrencyContext();
		ExampleSet exampleSet = BeltConverter.convert(ioTable,context);
		trainingHeader = new HeaderExampleSet(exampleSet);
		centroids = getCentroids(exampleSet);
		clusterSize = getClusterSize(exampleSet);

	}

	@Override
	protected Column performPrediction(Table adapted, Map<String, Column> confidences, Operator operator) throws OperatorException {
		SequentialConcurrencyContext context = new SequentialConcurrencyContext();

		ExampleSet exampleSet = BeltConverter.convert(new IOTable(adapted),context);
		NumericBuffer buffer = evaluate(exampleSet);
		return buffer.toColumn();
	}


	public abstract NumericBuffer evaluate(ExampleSet testSet) throws OperatorException;


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
			NominalMapping originalMapping = trainingHeader.getAttributes().getCluster().getMapping();
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
		NominalMapping originalMapping = trainingHeader.getAttributes().getCluster().getMapping();
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
		NominalMapping originalMapping = trainingHeader.getAttributes().getCluster().getMapping();
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
