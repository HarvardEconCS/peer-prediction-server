package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;

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
		return new double[]{first, 1 - first};
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

}
