package org.wikipedia.miner.util;

import java.util.List;

import org.apache.commons.math.stat.correlation.SpearmansCorrelation;

public class CorrelationCalculator {

	public static double getCorrelation(List<Double> colA, List<Double> colB) {
		
		SpearmansCorrelation sc = new SpearmansCorrelation();
		
		return sc.correlation(toArray(colA), toArray(colB));
	}
	
	private static double[] toArray(List<Double> col) {
		
		double vals[] = new double[col.size()];
		
		int index = 0;
		
		for (Double val:col) {
			vals[index] = val;
			index++;
		}
		
		return vals;
	}
}
