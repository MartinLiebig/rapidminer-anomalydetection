/**
 * RapidMiner Operator Toolbox Extension
 *
 * Copyright (C) 2016-2021 RapidMiner GmbH
 */
package com.rapidminer.extension.anomalydetection.utility;

public class Histogram {

	private double[] frequency;
	private double[] borders;
	private int bins;

	public Histogram(){}
	/**
	 * Creates a new histogram between min and max with bins number of bins + overflow and underflow
	 * @param min
	 * @param max
	 * @param bins
	 */
	public Histogram(double min, double max, int bins){
		this.bins = bins;
		double binWidth = Math.abs(max-min)/bins;
		frequency = new double[bins+2];
		for(int k = 0; k < frequency.length; k++){
			frequency[k] = 0;
		}
		borders = new double[bins+1];
		for(int i = 0; i<bins+1;++i){
			borders[i] = min+i*binWidth;
		}
	}

	public double getFrequency(int index){
		return frequency[index];
	}
	public double getFrequency(double value){
		return frequency[getIndex(value)];
	}

	public int getIndex(double value) {
		int index = 0; // 0 is underflow
		for(double b : borders){
			if (value <= b){
				return index;
			}
			index++;
		}
		// overflow
		return frequency.length-1;
	}

	public void add(double value){
		add(value,1);
	}

	public void add(double value, double weight){
		frequency[getIndex(value)] += weight;
	};
}
