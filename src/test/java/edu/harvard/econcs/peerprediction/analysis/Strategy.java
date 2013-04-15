package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;

public class Strategy {

	Map<String, Map<String, Double>> str;
	String label;

	public Strategy(Map<String, Map<String, Double>> strategy) {
		this.str = strategy;
		updateLabel();
	}

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
	
	public double getLogLikelihood(List<Pair<String, String>> signalReportPairs) {
		double prob = 0;
		for (int j = 0; j < signalReportPairs.size(); j++) {
			String signal = signalReportPairs.get(j).t1;
			String report = signalReportPairs.get(j).t2;
			double logProb = Math.log(str.get(signal).get(report));
			prob = prob + logProb;
		}
		return prob;
	}

	public double getLikelihood(List<Pair<String, String>> signalReportPairs) {
		double prob = 1.0;
		for (int j = 0; j < signalReportPairs.size(); j++) {
			String signal = signalReportPairs.get(j).t1;
			String report = signalReportPairs.get(j).t2;
			double logProb = str.get(signal).get(report);
			prob = prob * logProb;
		}
		return prob;
	}

	public double getLogLikelihood(List<String> signals, List<String> reports) {
		double prob = 0;
		for (int j = 0; j < signals.size(); j++) {
			String signal = signals.get(j);
			String report = reports.get(j);
			double logProb = Math.log(str.get(signal).get(report));
			prob = prob + logProb;
		}
		return prob;
	}

	public double getPercent(String signal, String report) {
		return str.get(signal).get(report);
	}

	public boolean isHonest() {
		return Math.abs(str.get("MM").get("MM") - 1) < AnalysisUtils.eps
				&& Math.abs(str.get("GB").get("GB") - 1) < AnalysisUtils.eps;
	}

	public boolean isOpposite() {
		return Math.abs(str.get("MM").get("GB") - 1) < AnalysisUtils.eps
				&& Math.abs(str.get("GB").get("MM") - 1) < AnalysisUtils.eps;
	}

	public boolean isGB() {
		return Math.abs(str.get("MM").get("GB") - 1) < AnalysisUtils.eps
				&& Math.abs(str.get("GB").get("GB") - 1) < AnalysisUtils.eps;
	}

	public boolean isMM() {
		return Math.abs(str.get("MM").get("MM") - 1) < AnalysisUtils.eps
				&& Math.abs(str.get("GB").get("MM") - 1) < AnalysisUtils.eps;
	}

	public boolean isCloseToMM(double threshold) {
		return str.get("MM").get("MM") >= threshold && str.get("GB").get("MM") >= threshold;
	}

	public boolean isCloseToHonest(double threshold) {
		return str.get("MM").get("MM") >= threshold && str.get("GB").get("GB") >= threshold;
	}

	public boolean isCloseToGB(double threshold) {
		return str.get("MM").get("GB") >= threshold && str.get("GB").get("GB") >= threshold;
	}

	public String toString() {
		return this.label;
	}

	public void setPercent(String signal, String report, double percent) {
		str.get(signal).put(report, percent);
		updateLabel();
	}

	private void updateLabel() {
		this.label = String.format("(%.4f-%.4f)", str.get("MM").get("MM"), str
				.get("GB").get("MM"));
		
	}

	public static Strategy getRandomStrategy() {
		Random rnd = new Random();
		double first = rnd.nextDouble();
		if (first < AnalysisUtils.eps || (1 - first) < AnalysisUtils.eps)
			first = rnd.nextDouble();
		double second = rnd.nextDouble();
		if (second < AnalysisUtils.eps || (1 - second) < AnalysisUtils.eps)
			second = rnd.nextDouble();
		return new Strategy(first, second);
	}
	
}
