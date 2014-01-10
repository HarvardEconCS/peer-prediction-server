package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import org.apache.commons.math3.util.ArithmeticUtils;

import be.ac.ulg.montefiore.run.jahmm.Opdf;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.SigActObservation;

public class AnalysisUtils {

	static final double epsConstruct = 0.01;
	static final double eps = 0.0000001;
	public static final String[] signalList = new String[] { "MM", "GB" };

	static int em_N;
	static int em_K;
	static double[] em_pi;
	static Strategy[] em_strategies;
	static double em_likelihood;

	static Random rand = new Random();

	public static Map<String, Double> getOppPopStr(List<String> otherReports) {
		Map<String, Integer> tempMap = new HashMap<String, Integer>();
		tempMap.put("MM", 0);
		tempMap.put("GB", 0);

		for (String report : otherReports) {
			int num = tempMap.get(report);
			num++;
			tempMap.put(report, num);
		}

		Map<String, Double> oppPopStrategy = new HashMap<String, Double>();
		int total = tempMap.get("MM") + tempMap.get("GB");
		oppPopStrategy.put("MM", tempMap.get("MM") * 1.0 / total);
		oppPopStrategy.put("GB", tempMap.get("GB") * 1.0 / total);
		return oppPopStrategy;
	}

	/**
	 * EM Algorithms
	 * 
	 * @param signalReportPairs
	 */
	public static void runEMAlgorithm(
			List<List<Pair<String, String>>> signalReportPairs) {

		int N = signalReportPairs.size();

		int K = AnalysisUtils.em_K;

		if (AnalysisUtils.em_pi == null) {
			em_pi = new double[K];
			for (int k = 0; k < K; k++) {
				em_pi[k] = 1.0 / K;
			}
		}

		if (AnalysisUtils.em_strategies == null) {
			em_strategies = new Strategy[K];
			for (int k = 0; k < K; k++) {
				em_strategies[k] = Strategy.getRandomStrategy();
			}
		}

		em_likelihood = AnalysisUtils.getLogLikelihood(signalReportPairs,
				em_pi, em_strategies);

		while (true) {

			// E step
			double[] pi_temp = new double[K];

			double[][] p = new double[N][K];
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < K; k++) {
					p[i][k] = em_pi[k];
					for (Pair<String, String> onePair : signalReportPairs
							.get(i)) {
						String signal = onePair.t1;
						String report = onePair.t2;
						p[i][k] *= em_strategies[k].getPercent(signal, report);
					}
				}
			}

			double[][] gamma = new double[N][K];
			for (int i = 0; i < N; i++) {
				double d_i = 0.0;
				for (int k = 0; k < K; k++) {
					d_i += p[i][k];
				}
				for (int k = 0; k < K; k++) {
					gamma[i][k] = p[i][k] / d_i;
				}
			}

			for (int k = 0; k < K; k++) {
				double n_k = 0.0;
				for (int i = 0; i < N; i++) {
					n_k += gamma[i][k];
				}
				pi_temp[k] = n_k / N;
			}

			// M step
			Strategy[] strategies_temp = new Strategy[K];

			for (int k = 0; k < K; k++) {
				double d_MM = 0.0;
				double d_GB = 0.0;
				double n_MM = 0.0;
				double n_GB = 0.0;

				for (int i = 0; i < N; i++) {
					int c_MMsignal = 0;
					int c_GBsignal = 0;
					int c_MMtoMM = 0;
					int c_GBtoMM = 0;

					for (Pair<String, String> onePair : signalReportPairs
							.get(i)) {
						if (onePair.t1.equals("MM")) {
							c_MMsignal++;
							if (onePair.t2.equals("MM")) {
								c_MMtoMM++;
							}
						} else {
							c_GBsignal++;
							if (onePair.t2.equals("MM")) {
								c_GBtoMM++;
							}
						}
					}

					d_MM += gamma[i][k] * c_MMsignal;
					d_GB += gamma[i][k] * c_GBsignal;
					n_MM += gamma[i][k] * c_MMtoMM;
					n_GB += gamma[i][k] * c_GBtoMM;
				}

				if (strategies_temp[k] == null)
					strategies_temp[k] = new Strategy(0.5, 0.5);

				strategies_temp[k].setPercent("MM", "MM", n_MM / d_MM);
				strategies_temp[k].setPercent("MM", "GB", 1 - (n_MM / d_MM));

				strategies_temp[k].setPercent("GB", "MM", n_GB / d_GB);
				strategies_temp[k].setPercent("GB", "GB", 1 - (n_GB / d_GB));
			}

			double likelihood_temp = AnalysisUtils.getLogLikelihood(
					signalReportPairs, pi_temp, strategies_temp);

			// System.out.println("loglk: " + logLikelihood);

			if (Math.abs((likelihood_temp - em_likelihood) / em_likelihood) < eps) {
				break;
			} else {
				em_likelihood = likelihood_temp;
				em_pi = pi_temp;
				em_strategies = strategies_temp;
			}

		}
	}

	private static double getLogLikelihood(
			List<List<Pair<String, String>>> signalReportPairs, double[] pk,
			Strategy[] strategies) {

		double logLikelihood = 0.0;
		for (List<Pair<String, String>> xi : signalReportPairs) {
			double sumOverK = 0.0;
			for (int k = 0; k < pk.length; k++) {
				double addend = pk[k];
				for (Pair<String, String> onePair : xi) {
					String signal = onePair.t1;
					String report = onePair.t2;
					addend *= strategies[k].getPercent(signal, report);
				}
				sumOverK += addend;
			}
			logLikelihood += Math.log(sumOverK);
		}
		return logLikelihood;
	}

	public static double[] getRandomTwoVec() {
		double first = rand.nextDouble();
		return new double[] { first, 1 - first };
	}

	public static double[] getRandomVec(int length) {
		List<Double> list = getRandomList(length, 1.0);
		double[] vec = new double[length];
		for (int i = 0; i < length; i++) {
			vec[i] = list.get(i).doubleValue();
		}
		return vec;
	}

	public static List<Double> getRandomList(int length, double remaining) {
		List<Double> list = new ArrayList<Double>();
		if (length > 1) {
			double num = remaining * rand.nextDouble();
			list = getRandomList(length - 1, remaining - num);
			list.add(num);
			return list;
		} else {
			list.add(remaining);
			return list;
		}

	}

	public static double getProbMM(double[] priorProbs,
			List<Map<String, Double>> prior) {
		double total = 0.0;
		for (int i = 0; i < prior.size(); i++) {
			double probWorld = priorProbs[i];
			double probMM = prior.get(i).get("MM");
			total += probWorld * probMM;
		}
		return total;
	}

	public static double getProbSignalsGivenSignal(int numMM, int numGB,
			String signal, double[] priorProbs, List<Map<String, Double>> prior) {

		double total0 = AnalysisUtils.getProbWorldGivenSignal(priorProbs,
				prior, 0, signal)
				* Math.pow(prior.get(0).get("MM"), numMM)
				* Math.pow(prior.get(0).get("GB"), numGB)
				* ArithmeticUtils.binomialCoefficient(3, numMM);
		double total1 = AnalysisUtils.getProbWorldGivenSignal(priorProbs,
				prior, 1, signal)
				* Math.pow(prior.get(1).get("MM"), numMM)
				* Math.pow(prior.get(1).get("GB"), numGB)
				* ArithmeticUtils.binomialCoefficient(3, numMM);
		return total0 + total1;
	}

	public static double getProbSignalGivenSignal(String resultSignal,
			String givenSignal, double[] priorProbs, List<Map<String, Double>> prior) {
		double total0 = AnalysisUtils.getProbWorldGivenSignal(priorProbs, prior, 0, givenSignal)
				* prior.get(0).get(resultSignal);
		double total1 = AnalysisUtils.getProbWorldGivenSignal(priorProbs, prior, 1, givenSignal)
				* prior.get(1).get(resultSignal);
		return total0 + total1;
	}

	public static double getProbWorldGivenSignal(double[] priorProbs,
			List<Map<String, Double>> prior, int i, String signal) {
		double probSignalAndWorld = priorProbs[i] * prior.get(i).get(signal);
		double probSignal = priorProbs[0] * prior.get(0).get(signal)
				+ priorProbs[1] * prior.get(1).get(signal);
		return probSignalAndWorld / probSignal;
	}

	public static double getTruthfulPayoff(String rule, double[] priorProbs,
			List<Map<String, Double>> prior) {
		double probMM = AnalysisUtils.getProbMM(priorProbs, prior);
		double probGB = 1 - probMM;

		double prob3MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(3, 0,
				"MM", priorProbs, prior);
		double prob2MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(2, 1,
				"MM", priorProbs, prior);
		double prob1MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(1, 2,
				"MM", priorProbs, prior);
		double prob0MMGivenMM = AnalysisUtils.getProbSignalsGivenSignal(0, 3,
				"MM", priorProbs, prior);

		double prob3MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(3, 0,
				"GB", priorProbs, prior);
		double prob2MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(2, 1,
				"GB", priorProbs, prior);
		double prob1MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(1, 2,
				"GB", priorProbs, prior);
		double prob0MMGivenGB = AnalysisUtils.getProbSignalsGivenSignal(0, 3,
				"GB", priorProbs, prior);

		return probMM * ( prob3MMGivenMM * AnalysisUtils.getPayment(rule, "MM", 3)
						+ prob2MMGivenMM * AnalysisUtils.getPayment(rule, "MM", 2)
						+ prob1MMGivenMM * AnalysisUtils.getPayment(rule, "MM", 1) 
						+ prob0MMGivenMM * AnalysisUtils.getPayment(rule, "MM", 0))
			+ probGB * (  prob3MMGivenGB * AnalysisUtils.getPayment(rule, "GB", 3)
						+ prob2MMGivenGB * AnalysisUtils.getPayment(rule, "GB", 2)
						+ prob1MMGivenGB * AnalysisUtils.getPayment(rule, "GB", 1) 
						+ prob0MMGivenGB * AnalysisUtils.getPayment(rule, "GB", 0));

	}

	public static double getMixedPayoff(String rule, double[] priorProbs,
			List<Map<String, Double>> prior,
			double strategyMMGivenMM, double strategyMMGivenGB) {

//		System.out.printf("%.2f, %.2f", strategyMMGivenMM, strategyMMGivenGB);
		
		double probMM = AnalysisUtils.getProbMM(priorProbs, prior);
		double probGB = 1 - probMM;
	
		double probMMGivenMM = AnalysisUtils.getProbSignalGivenSignal("MM", "MM", priorProbs, prior);
		double probMMGivenGB = AnalysisUtils.getProbSignalGivenSignal("MM", "GB", priorProbs, prior);
		
		double prob3MM0GBRefReportsGivenMM = 
				Math.pow((probMMGivenMM * strategyMMGivenMM + (1 - probMMGivenMM) * strategyMMGivenGB), 3);
		double prob2MM1GBRefReportsGivenMM = 3 
				* Math.pow((probMMGivenMM * strategyMMGivenMM + (1 - probMMGivenMM) * strategyMMGivenGB), 2)
				* Math.pow((probMMGivenMM * (1-strategyMMGivenMM) + (1 - probMMGivenMM) * (1-strategyMMGivenGB)), 1);
		double prob1MM2GBRefReportsGivenMM = 3 
				* Math.pow((probMMGivenMM * strategyMMGivenMM + (1 - probMMGivenMM) * strategyMMGivenGB), 1)
				* Math.pow((probMMGivenMM * (1-strategyMMGivenMM) + (1 - probMMGivenMM) * (1-strategyMMGivenGB)), 2);
		double prob0MM3GBRefReportsGivenMM = 	
				Math.pow((probMMGivenMM * (1-strategyMMGivenMM) + (1 - probMMGivenMM) * (1-strategyMMGivenGB)), 3);

		double prob3MM0GBRefReportsGivenGB = 
				Math.pow((probMMGivenGB * strategyMMGivenMM + (1 - probMMGivenGB) * strategyMMGivenGB), 3);
		double prob2MM1GBRefReportsGivenGB = 3 
				* Math.pow((probMMGivenGB * strategyMMGivenMM + (1 - probMMGivenGB) * strategyMMGivenGB), 2)
				* Math.pow((probMMGivenGB * (1-strategyMMGivenMM) + (1 - probMMGivenGB) * (1-strategyMMGivenGB)), 1);
		double prob1MM2GBRefReportsGivenGB = 3 
				* Math.pow((probMMGivenGB * strategyMMGivenMM + (1 - probMMGivenGB) * strategyMMGivenGB), 1)
				* Math.pow((probMMGivenGB * (1-strategyMMGivenMM) + (1 - probMMGivenGB) * (1-strategyMMGivenGB)), 2);
		double prob0MM3GBRefReportsGivenGB = 	
				Math.pow((probMMGivenGB * (1-strategyMMGivenMM) + (1 - probMMGivenGB) * (1-strategyMMGivenGB)), 3);

//		System.out.printf("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f", 
//				prob3MM0GBRefReportsGivenMM, prob2MM1GBRefReportsGivenMM, prob1MM2GBRefReportsGivenMM, prob0MM3GBRefReportsGivenMM, 
//				prob3MM0GBRefReportsGivenGB, prob2MM1GBRefReportsGivenGB, prob1MM2GBRefReportsGivenGB, prob0MM3GBRefReportsGivenGB);

		return (probMM * strategyMMGivenMM + probGB * strategyMMGivenGB)
				* (		  prob3MM0GBRefReportsGivenMM * AnalysisUtils.getPayment(rule, "MM", 3) 
						+ prob2MM1GBRefReportsGivenMM * AnalysisUtils.getPayment(rule, "MM", 2)
						+ prob1MM2GBRefReportsGivenMM * AnalysisUtils.getPayment(rule, "MM", 1) 
						+ prob0MM3GBRefReportsGivenMM * AnalysisUtils.getPayment(rule, "MM", 0))
				+ (probMM * (1 - strategyMMGivenMM) + probGB * (1 - strategyMMGivenGB))
				* (       prob3MM0GBRefReportsGivenGB * AnalysisUtils.getPayment(rule, "GB", 3)
						+ prob2MM1GBRefReportsGivenGB * AnalysisUtils.getPayment(rule, "GB", 2)
						+ prob1MM2GBRefReportsGivenGB * AnalysisUtils.getPayment(rule, "GB", 1) 
						+ prob0MM3GBRefReportsGivenGB * AnalysisUtils.getPayment(rule, "GB", 0));
	}

	private static double getPayment(String rule ,String myReport, int numMMInRefReports) {
		if (rule.equals("T3"))
			return AnalysisUtils.getPaymentT3(myReport, numMMInRefReports);
		else if (rule.equals("T5"))
			return AnalysisUtils.getPaymentT5(myReport, numMMInRefReports);
		return -1;
	}

	public static double getPaymentT5(String myReport, int numMMOtherReports) {
		if (myReport.equals("MM")) {
			switch (numMMOtherReports) {
			case 0:
				return 0.10;
			case 1:
				return 0.10;
			case 2:
				return 1.50;
			case 3:
				return 0.15;
			}
	
		} else {
			switch (numMMOtherReports) {
			case 0:
				return 0.15;
			case 1:
				return 0.90;
			case 2:
				return 0.15;
			case 3:
				return 0.10;
			}
		}
		return -1;
	}

	public static double getPaymentT3(String myReport, int numMMOtherReports) {
		if (myReport.equals("MM")) {
			switch (numMMOtherReports) {
			case 0:
				return 0.9;
			case 1:
				return 0.1;
			case 2:
				return 1.5;
			case 3:
				return 0.8;
			}
	
		} else {
			switch (numMMOtherReports) {
			case 0:
				return 0.8;
			case 1:
				return 1.5;
			case 2:
				return 0.1;
			case 3:
				return 0.9;
			}
		}
		return -1;
	}

}
