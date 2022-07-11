package com.rapidminer.extension.anomalydetection.anomaly_models.clustering;

import java.lang.reflect.Field;
import java.util.Random;
import java.util.concurrent.atomic.AtomicLong;

import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import com.fasterxml.jackson.databind.util.StdConverter;
import com.rapidminer.adaption.belt.IOTable;
import com.rapidminer.belt.buffer.Buffers;
import com.rapidminer.belt.buffer.NumericBuffer;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.clustering.ClusterModel;
import com.rapidminer.tools.RandomGenerator;
import com.rapidminer.tools.math.similarity.DistanceMeasure;

import de.dfki.madm.anomalydetection.evaluator.cluster_based.NewCMGOSEvaluator;


public class CMGOSModel extends ClusterBasedAnomalyDetectionModel {


	int threads;
	int removeRuns;
	double probability;
	int cov_sampling;
	double percentage;
	double lambda;
	int cov;
	int h;
	int numberOfSubsets;
	int fastMCDPoints;
	int inititeration;
	@JsonDeserialize(converter = RandomGeneratorDeserializer.class)
	@JsonSerialize(converter = RandomGeneratorSerializer.class)
	RandomGenerator randomGenerator = RandomGenerator.getGlobalRandomGenerator();
			//TODO: This is a problem on deserialization, but not sure how much sense it makes to store it anyway
	//wouldn't it mean that there is a difference between retrieve and apply twice and retrieve apply plus retrieve apply??
	NewCMGOSEvaluator evaluator;
	boolean trained;

	private CMGOSModel() {
		super();
	}

	public CMGOSModel(IOTable ioTable, ClusterModel model, DistanceMeasure measure) throws OperatorException {
		super(ioTable, model, measure);
		trained = false;
	}

	@Override
	public NumericBuffer evaluate(ExampleSet testSet) throws OperatorException {
		// CMGOS uses clusterSize[] not just for normalization, so we need to recalculate it
		// on the test set.
		int[] belongsToCluster = getClusterIds(testSet);
		double[][] points = AnomalyUtilities.exampleSetToDoubleArray(testSet, trainingHeader.getAttributes(), true);
		if (!trained) {
			clusterSize = getClusterSize(testSet);
			evaluator = new NewCMGOSEvaluator(
					distanceMeasure, points, belongsToCluster,
					centroids, clusterSize, threads, removeRuns, probability, cov_sampling, randomGenerator,
					percentage, lambda, cov, h, numberOfSubsets, fastMCDPoints, inititeration);
			double original_score[] = evaluator.evaluate();
			trained = true;
			NumericBuffer buffer = Buffers.realBuffer(original_score.length);
			for(int i = 0; i < original_score.length;++i){
				buffer.set(i,original_score[i]);
			}
			return buffer;
		} else {

			double[] score = evaluator.score(points,belongsToCluster);
			NumericBuffer buffer = Buffers.realBuffer(score.length);
			for(int i = 0; i < score.length;++i){
				buffer.set(i,score[i]);
			}
			return buffer;
		}

	}

	public void setThreads(int threads) {
		this.threads = threads;
	}

	public void setRemoveRuns(int removeRuns) {
		this.removeRuns = removeRuns;
	}

	public void setProbability(double probability) {
		this.probability = probability;
	}

	public void setCov_sampling(int cov_sampling) {
		this.cov_sampling = cov_sampling;
	}

	public void setPercentage(double percentage) {
		this.percentage = percentage;
	}

	public void setLambda(double lambda) {
		this.lambda = lambda;
	}

	public void setCov(int cov) {
		this.cov = cov;
	}

	public void setH(int h) {
		this.h = h;
	}

	public void setNumberOfSubsets(int numberOfSubsets) {
		this.numberOfSubsets = numberOfSubsets;
	}

	public void setFastMCDPoints(int fastMCDPoints) {
		this.fastMCDPoints = fastMCDPoints;
	}

	public void setInititeration(int inititeration) {
		this.inititeration = inititeration;
	}

	public void setRandomGenerator(RandomGenerator randomGenerator) {
		this.randomGenerator = randomGenerator;
	}

//TODO: this might require to sign the extension
	/**
	 * Converter from random generator to its seed.
	 */
	public static class RandomGeneratorSerializer extends StdConverter<RandomGenerator, Long> {

		@Override
		public Long convert(RandomGenerator value) {
			try {
				Field seed = Random.class.getDeclaredField("seed");
				seed.setAccessible(true);
				AtomicLong atomicLong = (AtomicLong) seed.get(value);
				return atomicLong.get();
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// cannot happen, called with permissions and field is there
				return 0L;
			}
		}
	}

	/**
	 * Converter from long value to random generator with that value as seed.
	 */
	public static class RandomGeneratorDeserializer extends StdConverter<Long, RandomGenerator> {

		@Override
		public RandomGenerator convert(Long value) {
			RandomGenerator generator = new RandomGenerator(value);
			try {
				Field seed = Random.class.getDeclaredField("seed");
				seed.setAccessible(true);
				AtomicLong atomicLong = (AtomicLong) seed.get(generator);
				atomicLong.set(value);
			} catch (NoSuchFieldException | IllegalAccessException e) {
				// cannot happen, called with permissions and field is there
			}
			return generator;
		}
	}
}
