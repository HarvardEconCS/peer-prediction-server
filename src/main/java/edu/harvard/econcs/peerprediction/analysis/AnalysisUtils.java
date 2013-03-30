package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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

	public static double[] calcPosteriorProb(double[] pi,
			Strategy[] strategies,
			List<List<Pair<String, String>>> signalReportPairs) {

		int N = signalReportPairs.size();

		int K = strategies.length;

		double[][] p = new double[N][K];
		for (int i = 0; i < N; i++) {
			for (int k = 0; k < K; k++) {
				p[i][k] = pi[k];
				for (Pair<String, String> onePair : signalReportPairs.get(i)) {
					String signal = onePair.t1;
					String report = onePair.t2;
					p[i][k] *= strategies[k].getPercent(signal, report);
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

		double[] pi_return = new double[K];
		for (int k = 0; k < K; k++) {
			double n_k = 0.0;
			for (int i = 0; i < N; i++) {
				n_k += gamma[i][k];
			}
			pi_return[k] = n_k / N;
		}

		return pi_return;
	}

	
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

	public static Strategy getMixedStrategy(
			List<Pair<String, String>> signalReportPairs, String[] signalList) {
		Map<String, Map<String, Double>> strategy = new HashMap<String, Map<String, Double>>();

		for (int i = 0; i < signalList.length; i++) {
			Map<String, Double> value = new HashMap<String, Double>();
			for (int j = 0; j < signalList.length; j++) {
				value.put(signalList[j], 0.0);
			}
			strategy.put(signalList[i], value);
		}

		for (Pair<String, String> p : signalReportPairs) {
			double count = strategy.get(p.t1).get(p.t2);
			count++;
			strategy.get(p.t1).put(p.t2, count);
		}

		for (int i = 0; i < signalList.length; i++) {
			double totalCount = 0;
			for (int j = 0; j < signalList.length; j++) {
				totalCount = totalCount
						+ strategy.get(signalList[i]).get(signalList[j]);
			}
			if (totalCount < AnalysisUtils.eps)
				for (int j = 0; j < signalList.length; j++) {
					strategy.get(signalList[i]).put(signalList[j], Double.NaN);
				}
			else {
				for (int j = 0; j < signalList.length; j++) {
					double thisCount = strategy.get(signalList[i]).get(
							signalList[j]);
					double percent = thisCount / totalCount;
					percent = (double) Math.round(percent * 100) / 100;
					strategy.get(signalList[i]).put(signalList[j], percent);
				}
			}
		}
		return new Strategy(strategy);
	}

	public static Strategy getStrategyForRound(Experiment expSet, int roundNum) {

		String[] signalList = AnalysisUtils.signalList;
		List<Pair<String, String>> signalReportPairs = expSet
				.getSignalReportPairsForRound(roundNum);
		Strategy strategy = AnalysisUtils.getMixedStrategy(signalReportPairs,
				signalList);

		return strategy;
	}

	public static Strategy getStrategyForRoundExcludeWorkers(Experiment expSet,
			int roundNum, List<String> workersToExclude) {
		String[] signalList = AnalysisUtils.signalList;
		List<Pair<String, String>> signalReportPairs = expSet
				.getSignalReportPairsForRoundExcludeWorkers(roundNum,
						workersToExclude);
		Strategy strategy = AnalysisUtils.getMixedStrategy(signalReportPairs,
				signalList);

		return strategy;
	}

	public static Strategy getStrategyForPlayer(Game game, String hitId) {

		String[] signalList = AnalysisUtils.signalList;
		List<Pair<String, String>> signalReportPairs = game
				.getSignalReportPairsForPlayer(hitId);
		Strategy strategy = AnalysisUtils.getMixedStrategy(signalReportPairs,
				signalList);

		return strategy;
	}

	public static Strategy getHonestStrategy() {
		Map<String, Map<String, Double>> strMap = new HashMap<String, Map<String, Double>>();

		Map<String, Double> mmValue = new HashMap<String, Double>();
		mmValue.put("MM", 1 - AnalysisUtils.epsConstruct);
		mmValue.put("GB", AnalysisUtils.epsConstruct);
		strMap.put("MM", mmValue);

		Map<String, Double> gbValue = new HashMap<String, Double>();
		gbValue.put("MM", AnalysisUtils.epsConstruct);
		gbValue.put("GB", 1 - AnalysisUtils.epsConstruct);
		strMap.put("GB", gbValue);

		Strategy honest = new Strategy(strMap);

		return honest;
	}

	public static Strategy getMMStrategy() {
		Map<String, Map<String, Double>> strMap = new HashMap<String, Map<String, Double>>();

		Map<String, Double> mmValue = new HashMap<String, Double>();
		mmValue.put("MM", 1 - AnalysisUtils.epsConstruct);
		mmValue.put("GB", AnalysisUtils.epsConstruct);
		strMap.put("MM", mmValue);

		Map<String, Double> gbValue = new HashMap<String, Double>();
		gbValue.put("MM", 1 - AnalysisUtils.epsConstruct);
		gbValue.put("GB", AnalysisUtils.epsConstruct);
		strMap.put("GB", gbValue);

		Strategy mm = new Strategy(strMap);
		return mm;
	}

	public static Strategy getGBStrategy() {
		Map<String, Map<String, Double>> strMap = new HashMap<String, Map<String, Double>>();

		Map<String, Double> mmValue = new HashMap<String, Double>();
		mmValue.put("MM", AnalysisUtils.epsConstruct);
		mmValue.put("GB", 1 - AnalysisUtils.epsConstruct);
		strMap.put("MM", mmValue);

		Map<String, Double> gbValue = new HashMap<String, Double>();
		gbValue.put("MM", AnalysisUtils.epsConstruct);
		gbValue.put("GB", 1 - AnalysisUtils.epsConstruct);
		strMap.put("GB", gbValue);

		Strategy gb = new Strategy(strMap);
		return gb;
	}

	public static Strategy getOppositeStrategy() {
		Map<String, Map<String, Double>> strMap = new HashMap<String, Map<String, Double>>();

		Map<String, Double> mmValue = new HashMap<String, Double>();
		mmValue.put("MM", AnalysisUtils.epsConstruct);
		mmValue.put("GB", 1 - AnalysisUtils.epsConstruct);
		strMap.put("MM", mmValue);

		Map<String, Double> gbValue = new HashMap<String, Double>();
		gbValue.put("MM", 1 - AnalysisUtils.epsConstruct);
		gbValue.put("GB", AnalysisUtils.epsConstruct);
		strMap.put("GB", gbValue);

		Strategy opposite = new Strategy(strMap);
		return opposite;
	}

	public static Strategy[] getPureStrategyArray() {
		return new Strategy[] { AnalysisUtils.getHonestStrategy(),
				AnalysisUtils.getMMStrategy(), AnalysisUtils.getGBStrategy(),
				AnalysisUtils.getOppositeStrategy() };
	}

	public static double[] getBestPureStrategies(
			List<Pair<String, String>> dataPoint) {
		Strategy[] pureStrategies = new Strategy[] {
				AnalysisUtils.getHonestStrategy(),
				AnalysisUtils.getMMStrategy(), AnalysisUtils.getGBStrategy(),
				AnalysisUtils.getOppositeStrategy() };
		double[] likelihoods = new double[] { 1.0, 1.0, 1.0, 1.0 };

		for (int i = 0; i < pureStrategies.length; i++) {
			likelihoods[i] = pureStrategies[i].getLikelihood(dataPoint);
		}

		List<Strategy> bestPureStrategies = new ArrayList<Strategy>();
		double bestLikelihood = 0.0;
		for (int i = 0; i < likelihoods.length; i++) {
			if (likelihoods[i] - bestLikelihood > AnalysisUtils.eps) {
				bestPureStrategies.clear();
				bestPureStrategies.add(pureStrategies[i]);
				bestLikelihood = likelihoods[i];
			} else if (Math.abs(likelihoods[i] - bestLikelihood) < AnalysisUtils.eps) {
				bestPureStrategies.add(pureStrategies[i]);
			}
		}

		double prob = 1.0 / bestPureStrategies.size();
		double[] dist = new double[pureStrategies.length];
		for (int i = 0; i < pureStrategies.length; i++) {
			if (bestPureStrategies.contains(pureStrategies[i])) {
				dist[i] = prob;
			} else {
				dist[i] = 0;
			}
		}

		return dist;
	}

}
