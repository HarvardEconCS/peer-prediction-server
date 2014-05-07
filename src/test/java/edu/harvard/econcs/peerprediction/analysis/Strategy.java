package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class Strategy {

	Map<String, Map<String, Double>> str;
	String label;

//	public Strategy(Map<String, Map<String, Double>> strategy) {
//		this.str = strategy;
//		updateLabel();
//	}

	public Strategy(double mmtomm, double gbtomm) {
		this.str = new HashMap<String, Map<String, Double>>();
		Map<String, Double> mmValue = new HashMap<String, Double>();
		mmValue.put("MM", mmtomm);
		mmValue.put("GB", 1 - mmtomm);
		str.put("MM", mmValue);

		Map<String, Double> gbValue = new HashMap<String, Double>();
		gbValue.put("MM", gbtomm);
		gbValue.put("GB", 1 - gbtomm);
		str.put("GB", gbValue);

		updateLabel();
	}
	

	public double getPercent(String signal, String report) {
		return str.get(signal).get(report);
	}

	public String toString() {
		return this.label;
	}

	public void setPercent(String signal, String report, double percent) {
		str.get(signal).put(report, percent);
		updateLabel();
	}

	private void updateLabel() {
		this.label = String.format("(%.2f,%.2f)", str.get("MM").get("MM"), str
				.get("GB").get("MM"));
		
	}

	public static Strategy getRandomStrategy() {
		Random rnd = new Random();
		double first = rnd.nextDouble();
		if (first < Utils.eps || (1 - first) < Utils.eps)
			first = rnd.nextDouble();
		double second = rnd.nextDouble();
		if (second < Utils.eps || (1 - second) < Utils.eps)
			second = rnd.nextDouble();
		return new Strategy(first, second);
	}
	
}
