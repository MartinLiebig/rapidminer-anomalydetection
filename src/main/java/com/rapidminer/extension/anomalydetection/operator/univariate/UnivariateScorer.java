/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;


public interface UnivariateScorer {

//	public UnivariateScorer();

	abstract void train(Column c, Context context);

	abstract double score(double value);
}
