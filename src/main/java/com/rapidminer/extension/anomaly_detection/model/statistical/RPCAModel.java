package com.rapidminer.extension.anomaly_detection.model.statistical;

import Jama.EigenvalueDecomposition;
import Jama.Matrix;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.example.Tools;
import com.rapidminer.example.set.MappedExampleSet;
import com.rapidminer.example.set.RemappedExampleSet;
import com.rapidminer.extension.anomaly_detection.model.AnomalyDetectionModel;
import com.rapidminer.operator.Operator;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.operator.UserError;
import com.rapidminer.operator.features.transformation.PCAModel;
import com.rapidminer.tools.math.matrix.CovarianceMatrix;

import org.apache.commons.lang.ArrayUtils;
import org.apache.commons.math.MathException;
import org.apache.commons.math.distribution.ChiSquaredDistributionImpl;

import java.util.*;


public class RPCAModel extends AnomalyDetectionModel {


	private double normProb = 0.05;


	private int reductionType = 1;
	private double varianceThreshold = 0.1;
	private int topmethod = 1;
	private int numberOfTopComponents = 1;
	private int lowMethod = 1;
	private int numberOfLowComponents = 1;
	private double valueThreshold = 0.1;

	public static final int PCS_ALL = 0;
	public static final int PCS_TOP = 1;
	public static final int PCS_LOWER = 2;
	public static final int PCS_BOTH = 3;


	public static final int PCS_TOP_VAR = 0;
	public static final int PCS_TOP_FIX = 1;

	public static final int PCS_LOW_VAL = 0;
	public static final int PCS_LOW_FIX = 1;

	// those are the 'model objects'
	private PCAModel model;
	private int[] opcs;
	double scoreNormalizer = 1.0;

	public RPCAModel(ExampleSet exampleSet) {
		super(exampleSet);
	}

	public void train(ExampleSet trainSet) throws OperatorException {
		Tools.onlyNonMissingValues(trainSet, "PCA");
		Tools.onlyNumericalAttributes(trainSet, "PCA");

		// Get normal probability.
		int olInst = trainSet.size() - (int) Math.floor(trainSet.size() * normProb);
		log("Ignoring " + olInst + " anomalyous instances for robustness.");
		// The robust estimate is based on removing top outliers first based on Mahalanobis distance (MD).
		// Since MD² is the same as the outlier score when using all PCs, the PCA is done twice:
		// First with all examples, second with top-outliers removed (robust)

		// First PCA for outlier removal
		// create covariance matrix
		Matrix covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(trainSet);
		// EigenVector and EigenValues of the covariance matrix
		EigenvalueDecomposition eigenvalueDecomposition = covarianceMatrix.eig();

		// create and deliver results
		double[] eigenvalues = eigenvalueDecomposition.getRealEigenvalues();
		Matrix eigenvectorMatrix = eigenvalueDecomposition.getV();
		double[][] eigenvectors = eigenvectorMatrix.getArray();
		model = new PCAModel(trainSet, eigenvalues, eigenvectors);

		// Perform transformation
		ExampleSet res = model.apply((ExampleSet) trainSet.clone());
		// Compute simple list with MDs and sort according to MD.
		List<double[]> l = new LinkedList<double[]>();
		double eIdx = 0;
		for (Example example : res) {
			double md = 0.0;
			int aNr = 0;
			for (Attribute attr : example.getAttributes()) {
				double pcscore = example.getValue(attr);
				md += (pcscore * pcscore) / model.getEigenvalue(aNr);
				aNr++;
			}
			double[] x = {md, eIdx};
			l.add(x);
			eIdx++;
		}
		Collections.sort(l, new Comparator<double[]>() {
			public int compare(double[] first, double[] second) {
				return Double.compare(second[0], first[0]);
			}
		});
		// Out of the list, create array with outlier-indexes and array (mapping) with good instances.
		Iterator<double[]> iter = l.iterator();
		int[] olMapping = new int[olInst];
		for (int i = 0; i < olInst; i++) {
			olMapping[i] = (int) ((double[]) iter.next())[1];
		}
		Arrays.sort(olMapping);
		int[] mapping = new int[trainSet.size() - olInst];
		int olc = 0;
		int ctr = 0;
		for (int i = 0; i < trainSet.size(); i++) {
			if (olc == olInst) { // Add last elements after last outlier
				mapping[ctr++] = i;
				continue;
			}
			if (olMapping[olc] != i) {
				mapping[ctr++] = i;
			} else {
				olc++;
			}
		}
		ExampleSet robustExampleSet = new MappedExampleSet(trainSet, mapping); // creates a new example set without the top outliers.
		// ---
		// Second PCA (robust)
		covarianceMatrix = CovarianceMatrix.getCovarianceMatrix(robustExampleSet);
		eigenvalueDecomposition = covarianceMatrix.eig();

		// create and deliver results
		eigenvalues = eigenvalueDecomposition.getRealEigenvalues();
		eigenvectorMatrix = eigenvalueDecomposition.getV();
		eigenvectors = eigenvectorMatrix.getArray();

		// Apply on original set
		model = new PCAModel(trainSet, eigenvalues, eigenvectors);


		// Sort eigenvalues
		Arrays.sort(eigenvalues);
		ArrayUtils.reverse(eigenvalues);


		List<Integer> pcList = new ArrayList<>();
		if (reductionType == PCS_ALL) {
			for (int i = 0; i < trainSet.getAttributes().size(); i++) {
				pcList.add(i);
			}
		}
		if (reductionType == PCS_TOP || reductionType == PCS_BOTH) {
			//top
			switch (topmethod) {
				case PCS_TOP_FIX:
					for (int i = 0; i < numberOfTopComponents; i++) {
						pcList.add(i);
					}
					break;
				case PCS_TOP_VAR:
					double var = varianceThreshold;
					boolean last = false;
					for (int i = 0; i < trainSet.getAttributes().size(); i++) {
						if (model.getCumulativeVariance(i) < var) {
							pcList.add(i);
						} else if (!last) { // we need to add another PC to meet the minimum requirement.
							last = true;
							pcList.add(i);
						}
					}
					break;
			}
		}
		if (reductionType == PCS_LOWER || reductionType == PCS_BOTH) {
			//lower
			switch (lowMethod) {
				case PCS_LOW_FIX:
					for (int i = trainSet.getAttributes().size() - numberOfLowComponents; i < trainSet.getAttributes().size(); i++) {
						pcList.add(i);
					}
					break;
				case PCS_LOW_VAL:
					double val = valueThreshold;
					for (int i = 0; i < eigenvalues.length; i++) {
						if (eigenvalues[i] <= val) {
							if (pcList.size() == 0) {
								pcList.add(i);
							} else if (pcList.get(pcList.size() - 1).intValue() < i) {
								pcList.add(i);
							}
						}
					}
					break;
			}
		}
		opcs = ArrayUtils.toPrimitive(pcList.toArray(new Integer[pcList.size()]));

		if (opcs.length == 0) {
			throw new OperatorException("Parameters thresholds are selected such that they did not match any principal component. Lower variance or increase eigenvalue threshold.");
		}
		if (opcs.length == trainSet.getAttributes().size()) {
			log("Using all PCs for score.");
		} else {
			log("Using following PCs for score: " + Arrays.toString(opcs));
		}

		// Normalize by Chi²-Dist with d degrees of freedom

		ChiSquaredDistributionImpl chi = new ChiSquaredDistributionImpl(opcs.length);
		try {
			scoreNormalizer = chi.inverseCumulativeProbability(normProb);
		} catch (MathException e) {
			throw new OperatorException(e.getMessage());
		}
		log("Normalizing score with chi² cumulative propability: " + scoreNormalizer);


	}


	@Override
	public double[] evaluate(ExampleSet testSet) throws OperatorException {

		ExampleSet res = model.apply((ExampleSet) testSet.clone());
		double[] score = new double[res.size()];

		for (int exNr = 0; exNr < testSet.size(); exNr++) {
			Example pc = res.getExample(exNr);
			double oscore = 0.0;
			int aNr = 0;
			int ctr = 0;
			for (Attribute attr : pc.getAttributes()) {
				if (ctr < opcs.length && opcs[ctr] != aNr) { // we skip this dimension
					aNr++;
					continue;
				}
				double pcscore = pc.getValue(attr);
				oscore += (pcscore * pcscore) / model.getEigenvalue(aNr);
				aNr++;
				ctr++;
			}
			score[exNr] = oscore / scoreNormalizer; //orig.setValue(scoreAttr, oscore/scoreNormalizer);
		}
		return score;
	}

	public void setNormProb(double normProb) {
		this.normProb = normProb;
	}

	public void setReductionType(int reductionType) {
		this.reductionType = reductionType;
	}

	public void setVarianceThreshold(double varianceThreshold) {
		this.varianceThreshold = varianceThreshold;
	}

	public void setTopmethod(int topmethod) {
		this.topmethod = topmethod;
	}

	public void setNumberOfTopComponents(int numberOfTopComponents) {
		this.numberOfTopComponents = numberOfTopComponents;
	}

	public void setLowMethod(int lowMethod) {
		this.lowMethod = lowMethod;
	}

	public void setNumberOfLowComponents(int numberOfLowComponents) {
		this.numberOfLowComponents = numberOfLowComponents;
	}

	public void setValueThreshold(double valueThreshold) {
		this.valueThreshold = valueThreshold;
	}

	@Override
	public String toString() {
		return model.toResultString();
	}
}
