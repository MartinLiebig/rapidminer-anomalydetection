package com.rapidminer.extension.anomalydetection.operator.univariate;



import org.apache.commons.math3.stat.descriptive.rank.Percentile;

import com.rapidminer.belt.column.Column;
import com.rapidminer.belt.execution.Context;
import com.rapidminer.extension.anomalydetection.utility.AnomalyUtilities;


/**
 * A simple scorer calculating percentiles and then using them to flag outliers.
 * this class returns the distance to the respective percentile if its above the threshold, else otherwise.
 * the scoringmode can be used to limit this to top/bottom outliers or take both.
 *
 * @author mliebig
 */
public class PercentileThresholdScorer implements UnivariateScorer {


	private double lowerPercentile;
	private double upperPercentile;


	private double percentileThreshold = 0.05;

	private String scoringMode = AnomalyUtilities.SCORING_MODE_BOTH;

	@Override
	public void train(Column c, Context context) {

		double[] data = new double[c.size()];
		c.fill(data, 0);
		Percentile p = new Percentile();
		p.setData(data);
		lowerPercentile = p.evaluate(percentileThreshold * 100);
		upperPercentile = p.evaluate((1 - percentileThreshold) * 100);
	}

	@Override
	public double score(double value) {
		if (value > upperPercentile && (scoringMode.equals(AnomalyUtilities.SCORING_MODE_BOTH) || scoringMode.equals(AnomalyUtilities.SCORING_MODE_ONLY_TOP))) {
			return Math.abs(value - upperPercentile);
		} else if (value < lowerPercentile && (scoringMode.equals(AnomalyUtilities.SCORING_MODE_BOTH) || scoringMode.equals(AnomalyUtilities.SCORING_MODE_ONLY_BOTTOM))) {
			return Math.abs(value - lowerPercentile);
		} else {
			return 0;
		}

	}

	public double getPercentileThreshold() {
		return percentileThreshold;
	}

	public void setPercentileThreshold(double percentileThreshold) {
		this.percentileThreshold = percentileThreshold;
	}

	public String getScoringMode() {
		return scoringMode;
	}

	public void setScoringMode(String scoringMode) {
		this.scoringMode = scoringMode;
	}

}
