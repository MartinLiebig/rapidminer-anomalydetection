/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.operator.univariate;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;

@JsonTypeInfo(
		use = JsonTypeInfo.Id.NAME,
		include = JsonTypeInfo.As.PROPERTY,
		property = "type",
		defaultImpl = ZScorer.class
)
@JsonSubTypes({
		@JsonSubTypes.Type(value = ZScorer.class, name = "zscorer"),
		@JsonSubTypes.Type(value = QuartileScorer.class, name = "quartilescorer"),
		@JsonSubTypes.Type(value = HistogramBasedScorer.class, name = "histogramscorer"),
		@JsonSubTypes.Type(value = PercentileThresholdScorer.class, name = "percentilethreshold")
})
public interface UnivariateScorer {

	abstract void train(Column c, Context context);

	abstract double score(double value);
}
