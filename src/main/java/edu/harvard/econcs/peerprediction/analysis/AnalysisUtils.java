package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.misc.Pair;

public class AnalysisUtils {

	static final double epsConstruct = 0.01;
	static final double eps = 0.0000001;

	public static void runEMAlgorithm(List<List<Pair<String, String>>> signalReportPairs, String[] signalList) {
		
		int N = signalReportPairs.size();
		
		int K = 4;
		double[] p_k = new double[K];
		for (int k = 0; k < p_k.length; k++)
			p_k[k] = 1.0 / K;
		
		Strategy[] strategies = new Strategy[] {
				AnalysisUtils.getHonestStrategy(),
				AnalysisUtils.getMMStrategy(), 
				AnalysisUtils.getGBStrategy(),
				AnalysisUtils.getOppositeStrategy() 
				};
		
		double logLikelihood = AnalysisUtils.getLogLikelihood(signalReportPairs, p_k, strategies);
		
		double eps = 0.000001;
		
		int count = 0;
		while (true) {
			count++;
			
			// E step
			double[][] pi_ik = new double[N][K];
			for (int i = 0; i < N; i++) {
				for (int k = 0; k < K; k++) {
					double PrXiGivenSk = 1.0;
					for (Pair<String, String> onePair : signalReportPairs.get(i)) {
						String signal = onePair.t1;
						String report = onePair.t2;
						PrXiGivenSk = PrXiGivenSk * strategies[k].getPercent(signal, report);
					}
					pi_ik[i][k] = PrXiGivenSk * p_k[k];
				}
			}
			
			double[][] p_ik = new double[N][K];
			for (int i = 0; i < N; i++) {
				double denom_i = 0.0;
				for (int k = 0; k < K; k++) {
					denom_i += pi_ik[i][k];
				}

				for (int k = 0; k < K; k++) {
					p_ik[i][k] = pi_ik[i][k] / denom_i;
				}
			}
			
			for (int k = 0; k < K; k++) {
				double numerator_k = 0.0;
				for (int i = 0; i < N; i++) {
					numerator_k += p_ik[i][k];
				}
				
				p_k[k] = numerator_k / N;
			}
			
			// M step
			for (int k = 0; k < K; k++) {
				double denom_MM = 0.0;
				double denom_GB = 0.0;
				
				double numerator_MM = 0.0;
				double numerator_GB = 0.0;
				
				for (int i = 0; i < N; i++) {
					int count_MM_signal = 0;
					int count_GB_signal = 0;
					
					int count_MM_signal_MM_report = 0;
					int count_GB_signal_MM_report = 0;
					
					for (Pair<String, String> onePair : signalReportPairs.get(i)) {
						if (onePair.t1.equals("MM")) {
							count_MM_signal++;
							if (onePair.t2.equals("MM")) {
								count_MM_signal_MM_report++;
							}
						} else {
							count_GB_signal++;
							if (onePair.t2.equals("MM")) {
								count_GB_signal_MM_report++;
							}
						}
					}
					denom_MM += p_ik[i][k] * count_MM_signal;
					denom_GB += p_ik[i][k] * count_GB_signal;
					
					numerator_MM += p_ik[i][k] * count_MM_signal_MM_report;
					numerator_GB += p_ik[i][k] * count_GB_signal_MM_report;
				}
				
				strategies[k].setPercent("MM", "MM", numerator_MM / denom_MM);
				strategies[k].setPercent("MM", "GB", 1 - (numerator_MM / denom_MM));				
				
				strategies[k].setPercent("GB", "MM", numerator_GB / denom_GB);
				strategies[k].setPercent("GB", "GB", 1 - (numerator_GB / denom_GB));				
			}
			
			double logLikelihood_new = AnalysisUtils.getLogLikelihood(signalReportPairs, p_k, strategies);
			
			if (Math.abs(logLikelihood_new - logLikelihood) < eps) {
				break;
			} else {
				logLikelihood = logLikelihood_new;
			}
		}
		
//		System.out.printf("iteration %d\n", count);
		System.out.printf("p_k %s\n", Arrays.toString(p_k));
		System.out.printf("strategies %s\n", Arrays.toString(strategies));
		
	}
	
	private static double getLogLikelihood(
			List<List<Pair<String, String>>> signalReportPairs, double[] pk,
			Strategy[] strategies) {
		
		double logLikelihood = 0.0;
		for (List<Pair<String, String>> xi : signalReportPairs) {
			double sumOverK = 0.0;
			for (int k = 0; k < pk.length; k++) {
				double PrXiGivenSk = 1.0;
				for (Pair<String, String> onePair : xi) {
					String signal = onePair.t1;
					String report = onePair.t2;
					PrXiGivenSk = PrXiGivenSk * strategies[k].getPercent(signal, report);
				}
				sumOverK = sumOverK + PrXiGivenSk;
			}
			logLikelihood = Math.log(sumOverK);
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
				totalCount = totalCount + strategy.get(signalList[i]).get(signalList[j]);
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
		
		String[] signalList = expSet.games.get(0).signalList;
		List<Pair<String, String>> signalReportPairs = 
				expSet.getSignalReportPairsForRound(roundNum);
		Strategy strategy = 
				AnalysisUtils.getMixedStrategy(signalReportPairs, signalList);
		
		return strategy;
	}
	
	public static Strategy getStrategyForPlayer(Game game, String hitId) {
	
		String[] signalList = game.signalList;
		List<Pair<String, String>> signalReportPairs = game.getSignalReportPairsForPlayer(hitId);
		Strategy strategy = AnalysisUtils.getMixedStrategy(signalReportPairs, signalList);
		
		return strategy;
	}


	public static List<Strategy> getBestPureStrategies(Strategy[] strategies,
			double[] likelihoods) {
		List<Strategy> bestStrategies = new ArrayList<Strategy>();
		double bestLikelihood = Double.NEGATIVE_INFINITY;
		
		for (int i = 0; i < strategies.length; i++) {
			if (likelihoods[i] - bestLikelihood > AnalysisUtils.eps) {
				bestStrategies.clear();
				bestStrategies.add(strategies[i]);
				bestLikelihood = likelihoods[i];
			} else if (Math.abs(likelihoods[i] - bestLikelihood) < AnalysisUtils.eps) {
				bestStrategies.add(strategies[i]);
			}
		}
		return bestStrategies;
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
		return new Strategy[] { 
				getHonestStrategy(),
				AnalysisUtils.getMMStrategy(), 
				AnalysisUtils.getGBStrategy(),
				AnalysisUtils.getOppositeStrategy() };
	}


}
