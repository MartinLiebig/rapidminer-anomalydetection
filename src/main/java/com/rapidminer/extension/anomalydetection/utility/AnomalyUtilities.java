package com.rapidminer.extension.anomalydetection.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;
import java.util.SplittableRandom;

import org.apache.commons.math3.util.Pair;

import com.rapidminer.belt.execution.Context;
import com.rapidminer.belt.reader.NumericReader;
import com.rapidminer.belt.reader.Readers;
import com.rapidminer.belt.table.Table;
import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.RandomGenerator;


public class AnomalyUtilities {

	private AnomalyUtilities(){};

	public static final String ANOMALY_SCORE_NAME = "score";
	public static final String ANOMALY_FLAG_NAME = "outlier_flag";
	public static final String ANOMALY_NAME = "Outlier";
	public static final String NO_ANOMALY_NAME = "No Outlier";

	private static Pair<double[][], double[]> convertExampleSetToDoubleArrays(ExampleSet exaSet,
																			  List<Attribute> attributeList, com.rapidminer.example.Attribute labelAttribute,
																			  boolean failOnMissing) throws OperatorException {
		double[][] valueMatrix = new double[exaSet.size()][attributeList.size()];
		double[] labelData = null;
		if (labelAttribute != null) {
			labelData = new double[exaSet.size()];
		}
		List<Attribute> attributesOnApplicationSet = new ArrayList<>();
		for (com.rapidminer.example.Attribute a : attributeList) {
			Attribute a2 = exaSet.getAttributes().get(a.getName());
			if (a2 == null) {
				throw new OperatorException("Attribute " + a.getName() + " is not available");
			}
			attributesOnApplicationSet.add(a2);
		}

		int exaCounter = 0;
		for (Example e : exaSet) {
			if (labelAttribute != null) {
				double value = e.getValue(labelAttribute);

				if (failOnMissing && Double.isNaN(value)) {
					throw new OperatorException(labelAttribute.getName());
				}

				labelData[exaCounter] = value;
			}
			int attCounter = 0;
			for (com.rapidminer.example.Attribute a : attributesOnApplicationSet) {
				double value = e.getValue(a);

				if (failOnMissing && Double.isNaN(value)) {
					throw new OperatorException(a.getName());
				}

				valueMatrix[exaCounter][attCounter] = e.getValue(a);
				attCounter++;
			}
			exaCounter++;
		}

		return new Pair<>(valueMatrix, labelData);
	}

	/**
	 * Extracts the double values for the provided Set of {@link com.rapidminer.example.Attribute}s
	 * from the {@link ExampleSet} and returns them as a 2-d double array (Examples x Attributes).
	 * Note that this method neither checks if the {@link com.rapidminer.example.Attribute} is part
	 * of the {@link ExampleSet}, if it has a specific value type nor if it has a special role. Also
	 * {@link com.rapidminer.example.table.NominalAttribute} can be used, but the returned values
	 * contain the integer mapping values.
	 *
	 * @param exaSet ExampleSet from which the data shall be extracted
	 * @param atts   Set of Attributes for which the data shall be extracted
	 * @return 2-d double array (Examples x Attributes) with the extracted data
	 */
	public static double[][] exampleSetToDoubleArray(ExampleSet exaSet, Set<Attribute> atts,
													 boolean failOnMissing) throws OperatorException {
		List<com.rapidminer.example.Attribute> convertedAtts = new LinkedList<>();
		convertedAtts.addAll(atts);
		return convertExampleSetToDoubleArrays(exaSet, convertedAtts, null, failOnMissing).getFirst();
	}

	/**
	 * Extracts the double values for the provided {@link com.rapidminer.example.Attributes} from
	 * the {@link ExampleSet} and returns them as a 2-d double array (Examples x Attributes). Note
	 * that this method neither checks if the {@link com.rapidminer.example.Attribute} is part of
	 * the {@link ExampleSet}, if it has a specific value type nor if it has a special role. Also
	 * {@link com.rapidminer.example.table.NominalAttribute} can be used, but the returned values
	 * contain the integer mapping values.
	 *
	 * @param exaSet ExampleSet from which the data shall be extracted
	 * @param atts   Attributes for which the data shall be extracted
	 * @return 2-d double array (Examples x Attributes) with the extracted data
	 */
	public static double[][] exampleSetToDoubleArray(ExampleSet exaSet, Attributes atts, boolean failOnMissing)
			throws OperatorException {
		List<com.rapidminer.example.Attribute> convertedAtts = new LinkedList<>();
		for (Iterator<AttributeRole> it = atts.regularAttributes(); it.hasNext(); ) {
			AttributeRole a = it.next();

			convertedAtts.add(a.getAttribute());
		}
		return convertExampleSetToDoubleArrays(exaSet, convertedAtts, null, failOnMissing).getFirst();
	}

	/**
	 * Bootstrap numberOfNewRows from a given table
	 * @param tableToBeBootstrapped the table you want to draw from
	 * @param numberOfNewRows number of rows of the original table
	 * @param rng Random Generator used
	 * @param beltContext context for execution
	 * @return a bootstrapped version of the old Table
	 */

	public static Table bootStrapTable(Table tableToBeBootstrapped, int numberOfNewRows, SplittableRandom rng, Context beltContext){
		int[] intArray = new int[numberOfNewRows];
		for (int k = 0; k < numberOfNewRows; ++k) {
			intArray[k] = rng.nextInt(0,tableToBeBootstrapped.height());
		}
		Table bootstrappedTable = tableToBeBootstrapped.rows(intArray, beltContext);
		return bootstrappedTable;
	}


	/**
	 * Computes the (interpolated) pth percentile of the given n (sorted) values. Please note that there is not a
	 * standard universally accepted way to interpolate percentiles. This implementation uses the method proposed by
	 * the National Institute of Standards and Technology (NIST) as described in its Engineering Statistics Handbook.
	 *
	 * @param reader
	 * 		the reader for the sorted values
	 * @param n
	 * 		the number of non-missing values
	 * @param p
	 * 		the percentile to compute
	 * @return the pth percentile
	 */
	public static double computePercentile(NumericReader reader, int n, double p) {
		double rank = p * (n + 1);
		int index = (int) rank;
		if (index < 1) {
			reader.setPosition(Readers.BEFORE_FIRST_ROW);
			return reader.read();
		} else if (index < n) {
			reader.setPosition(index - 2);
			double weight = rank - index;
			double value = reader.read();
			value += weight * (reader.read() - value);
			return value;
		} else {
			reader.setPosition(n - 2);
			return reader.read();
		}
	}

}
