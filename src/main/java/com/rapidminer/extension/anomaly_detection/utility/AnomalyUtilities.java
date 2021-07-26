package com.rapidminer.extension.anomaly_detection.utility;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.math3.util.Pair;

import com.rapidminer.example.Attribute;
import com.rapidminer.example.AttributeRole;
import com.rapidminer.example.Attributes;
import com.rapidminer.example.Example;
import com.rapidminer.example.ExampleSet;
import com.rapidminer.operator.OperatorException;
import com.rapidminer.tools.Tools;


public class AnomalyUtilities {
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

}
