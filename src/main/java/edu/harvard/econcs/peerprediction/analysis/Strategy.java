package edu.harvard.econcs.peerprediction.analysis;

import java.util.List;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class Strategy {

	Map<String, Map<String, Double>> str;
	String label;
	public Strategy(Map<String, Map<String, Double>> strategy) {
		this.str = strategy;
		this.label = String.format("(%.2f,%.2f)", 
				str.get("MM").get("MM"), 
				str.get("GB").get("MM"));
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

	public String toString() {
		return this.label;
	}

	public void setPercent(String signal, String report, double percent) {
		str.get(signal).put(report, percent);
		this.label = String.format("(%.2f,%.2f)", 
				str.get("MM").get("MM"), 
				str.get("GB").get("MM"));
	}

}
