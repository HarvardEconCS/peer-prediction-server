package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;

import org.apache.commons.math3.util.ArithmeticUtils;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

public class AnalysisUtils {

	static final double epsConstruct = 0.01;
	static final double eps = 0.0000001;
	public static final String[] signalList = new String[] { "MM", "GB" };

	public static final Gson gson = new Gson();
	
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

	public static void getSymmMixedStrEq3Players(String rule) {
		
		System.out.printf("\nSolving for symmetric mixed strategy equilibrium.  %s\n", rule);
		
		double[] priorProbs = new double[] { 0.5, 0.5 };
		List<Map<String, Double>> prior = new ArrayList<Map<String, Double>>();
		prior.add(ImmutableMap.of("MM", 0.2, "GB", 0.8));
		prior.add(ImmutableMap.of("MM", 0.7, "GB", 0.3));
		
		double minDiffGB = Double.POSITIVE_INFINITY;
		double minDiffMM = Double.POSITIVE_INFINITY;
		double[] strategy = new double[]{0.0, 0.0};
		
		double unit = 0.001;
		int numUnit = (int) (1 / unit);
		for (int i = 1; i < numUnit; i++) {
			for (int j = 1; j < numUnit; j++) {
				double strategyMMGivenMM = unit * i;
				double strategyMMGivenGB = unit * j;
				
				double payoffMMSignalMMReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport3Players(rule, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "MM", "MM");
				double payoffMMSignalGBReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport3Players(rule, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "MM", "GB");
				double diffMMSignal = Math.abs(payoffMMSignalMMReport - payoffMMSignalGBReport);
				
				double payoffForGBSignalMMReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport3Players(rule, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "GB", "MM");
				double payoffForGBSignalGBReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport3Players(rule, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "GB", "GB");
				double diffGBSignal = Math.abs(payoffForGBSignalMMReport - payoffForGBSignalGBReport);
				
				if (diffGBSignal < minDiffGB && diffMMSignal < minDiffMM) {
					minDiffGB = diffGBSignal;
					minDiffMM = diffMMSignal;
					strategy[0] = strategyMMGivenMM;
					strategy[1] = strategyMMGivenGB;
				}
			}
		}
		
		System.out.printf("Close to mixed strategy equilibrium is %s\n", Arrays.toString(strategy));
		System.out.printf("Best diff is %.4f for MM signal and %.4f for GB signal\n", minDiffMM, minDiffGB);
	}
	
	public static void getSymmMixedStrEq4Players(String treatment) {
		
		System.out.printf("\nSolving for symmetric mixed strategy equilibrium.  %s\n", treatment);
		
		double[] priorProbs = new double[] { 0.5, 0.5 };
		List<Map<String, Double>> prior = new ArrayList<Map<String, Double>>();
		prior.add(ImmutableMap.of("MM", 0.2, "GB", 0.8));
		prior.add(ImmutableMap.of("MM", 0.7, "GB", 0.3));
		
		double minDiffGB = Double.POSITIVE_INFINITY;
		double minDiffMM = Double.POSITIVE_INFINITY;
		double[] strategy = new double[]{0.0, 0.0};
		
		double unit = 0.001;
		int numUnit = (int) (1 / unit);
		for (int i = 1; i < numUnit; i++) {
			for (int j = 1; j < numUnit; j++) {
				double strategyMMGivenMM = unit * i;
				double strategyMMGivenGB = unit * j;
				
				double payoffMMSignalMMReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport4Players(treatment, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "MM", "MM");
				double payoffMMSignalGBReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport4Players(treatment, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "MM", "GB");
				double diffMMSignal = Math.abs(payoffMMSignalMMReport - payoffMMSignalGBReport);
				
				double payoffForGBSignalMMReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport4Players(treatment, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "GB", "MM");
				double payoffForGBSignalGBReport = 
						AnalysisUtils.getMixedPayoffForSignalAndReport4Players(treatment, priorProbs, prior, 
								strategyMMGivenMM, strategyMMGivenGB, "GB", "GB");
				double diffGBSignal = Math.abs(payoffForGBSignalMMReport - payoffForGBSignalGBReport);
				
				if (diffGBSignal < minDiffGB && diffMMSignal < minDiffMM) {
					minDiffGB = diffGBSignal;
					minDiffMM = diffMMSignal;
					strategy[0] = strategyMMGivenMM;
					strategy[1] = strategyMMGivenGB;
				}
			}
		}
		
		System.out.printf("Close to mixed strategy equilibrium is %s\n", Arrays.toString(strategy));
		System.out.printf("Best diff is %.4f for MM signal and %.4f for GB signal\n", minDiffMM, minDiffGB);
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

	public static double[] getRandomVec(int length) {
		List<Double> list = getRandomList(length);
		double[] array = new double[length];
		for (int i = 0; i < length; i++) {
			array[i] = list.get(i).doubleValue();
		}
		return array;
	}

	public static List<Double> getRandomList(int length) {
		List<Double> list = new ArrayList<Double>();
		for (int i = 1; i <= length - 1; i++) {
			list.add(rand.nextDouble());
		}
		list.add(0.0);
		list.add(1.0);
		Collections.sort(list);
		
		List<Double> returnList = new ArrayList<Double>();
		for (int i = 0; i < length; i++) {
			double value = list.get(i + 1).doubleValue() - list.get(i).doubleValue();
			returnList.add(value);
		}
		return returnList;
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

	public static double getTruthfulPayoff(String treatment, double[] priorProbs,
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

		return probMM * ( prob3MMGivenMM * AnalysisUtils.getPayment(treatment, "MM", 3)
						+ prob2MMGivenMM * AnalysisUtils.getPayment(treatment, "MM", 2)
						+ prob1MMGivenMM * AnalysisUtils.getPayment(treatment, "MM", 1) 
						+ prob0MMGivenMM * AnalysisUtils.getPayment(treatment, "MM", 0))
			+ probGB * (  prob3MMGivenGB * AnalysisUtils.getPayment(treatment, "GB", 3)
						+ prob2MMGivenGB * AnalysisUtils.getPayment(treatment, "GB", 2)
						+ prob1MMGivenGB * AnalysisUtils.getPayment(treatment, "GB", 1) 
						+ prob0MMGivenGB * AnalysisUtils.getPayment(treatment, "GB", 0));

	}

	private static double getMixedPayoffForSignalAndReport3Players(String treatment,
			double[] priorProbs, List<Map<String, Double>> prior,
			double strategyMMGivenMM, double strategyMMGivenGB, 
			String signal,
			String report) {

		double probMMRefReportGivenMM = 
				AnalysisUtils.getProbRefReportGivenSignalAndStrategy("MM", "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double probGBRefReportGivenMM = 
				AnalysisUtils.getProbRefReportGivenSignalAndStrategy("GB", "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		
		double probMMRefReportGivenGB = 
				AnalysisUtils.getProbRefReportGivenSignalAndStrategy("MM", "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double probGBRefReportGivenGB = 
				AnalysisUtils.getProbRefReportGivenSignalAndStrategy("GB", "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		
		if (signal.equals("MM") && report.equals("MM")) {
			return probMMRefReportGivenMM * AnalysisUtils.getPayment(treatment, "MM", "MM")
				 + probGBRefReportGivenMM * AnalysisUtils.getPayment(treatment, "MM", "GB");
		} else if (signal.equals("MM") && report.equals("GB")) {
			return probMMRefReportGivenMM * AnalysisUtils.getPayment(treatment, "GB", "MM")
				 + probGBRefReportGivenMM * AnalysisUtils.getPayment(treatment, "GB", "GB");			
		} else if (signal.equals("GB") && report.equals("MM")) {
			return probMMRefReportGivenGB * AnalysisUtils.getPayment(treatment, "MM", "MM")
				 + probGBRefReportGivenGB * AnalysisUtils.getPayment(treatment, "MM", "GB");			
		} else if (signal.equals("GB") && report.equals("GB")) {
			return probMMRefReportGivenGB * AnalysisUtils.getPayment(treatment, "GB", "MM")
				 + probGBRefReportGivenGB * AnalysisUtils.getPayment(treatment, "GB", "GB");
			
		}
		return -1;
	}

	public static double getMixedPayoffForSignalAndReport4Players(String treatment, 
			double[] priorProbs, List<Map<String, Double>> prior,
			double strategyMMGivenMM, double strategyMMGivenGB,
			String signal, String report) {
		
		double prob3MM0GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(3, 0, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(2, 1, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(1, 2, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenMM = 	
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(0, 3, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		double prob3MM0GBRefReportsGivenGB = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(3, 0, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenGB = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(2, 1, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenGB =  
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(1, 2, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenGB = 	
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(0, 3, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		
		if (signal.equals("MM") && report.equals("MM")) {
			return prob3MM0GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 3) 
				 + prob2MM1GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 2)
			 	 + prob1MM2GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 1) 
			 	 + prob0MM3GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 0);
		} else if (signal.equals("MM") && report.equals("GB")) {
			return prob3MM0GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 3)
				 + prob2MM1GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 2)
				 + prob1MM2GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 1) 
				 + prob0MM3GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 0);
		} else if (signal.equals("GB") && report.equals("MM")){
			return prob3MM0GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 3) 
				 + prob2MM1GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 2)
				 + prob1MM2GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 1) 
				 + prob0MM3GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 0);
		} else if (signal.equals("GB") && report.equals("GB")) {
			return prob3MM0GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 3)
				 + prob2MM1GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 2)
				 + prob1MM2GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 1) 
				 + prob0MM3GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 0);
		}
		return -1;
	}
	
	
	
	public static double getMixedPayoff(String treatment, double[] priorProbs,
			List<Map<String, Double>> prior,
			double strategyMMGivenMM, double strategyMMGivenGB) {

//		System.out.printf("%.2f, %.2f", strategyMMGivenMM, strategyMMGivenGB);
		
		double prob3MM0GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(3, 0, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(2, 1, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenMM = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(1, 2, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenMM = 	
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(0, 3, "MM", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		double prob3MM0GBRefReportsGivenGB = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(3, 0, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenGB = 
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(2, 1, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenGB =  
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(1, 2, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenGB = 	
				AnalysisUtils.getProbRefReportsGivenSignalAndStrategy(0, 3, "GB", 
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

//		System.out.printf("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f", 
//				prob3MM0GBRefReportsGivenMM, prob2MM1GBRefReportsGivenMM, prob1MM2GBRefReportsGivenMM, prob0MM3GBRefReportsGivenMM, 
//				prob3MM0GBRefReportsGivenGB, prob2MM1GBRefReportsGivenGB, prob1MM2GBRefReportsGivenGB, prob0MM3GBRefReportsGivenGB);

		double probMM = AnalysisUtils.getProbMM(priorProbs, prior);
		double probGB = 1 - probMM;
		
		return probMM * strategyMMGivenMM
				* (prob3MM0GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 3) 
				 + prob2MM1GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 2)
				 + prob1MM2GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 1) 
				 + prob0MM3GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "MM", 0))
			+ probGB * strategyMMGivenGB
			 	* (prob3MM0GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 3)
				 + prob2MM1GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 2)
				 + prob1MM2GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 1) 
				 + prob0MM3GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "MM", 0))
			+ probMM * (1 - strategyMMGivenMM) 
				* (prob3MM0GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 3)
				 + prob2MM1GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 2)
				 + prob1MM2GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 1) 
				 + prob0MM3GBRefReportsGivenMM * AnalysisUtils.getPayment(treatment, "GB", 0))
			+ probGB * (1 - strategyMMGivenGB)
				* (prob3MM0GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 3)
				 + prob2MM1GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 2)
				 + prob1MM2GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 1) 
				 + prob0MM3GBRefReportsGivenGB * AnalysisUtils.getPayment(treatment, "GB", 0))
			;
	}

	public static double getProbRefReportGivenSignalAndStrategy(
			String refReport, String signal, 
			double strategyMMGivenMM, double strategyMMGivenGB,
			double[] priorProbs, List<Map<String, Double>> prior) {
		
		double probMMGivenMM = AnalysisUtils.getProbSignalGivenSignal("MM", "MM", priorProbs, prior);
		double probGBGivenMM = 1 - probMMGivenMM;
		double probMMGivenGB = AnalysisUtils.getProbSignalGivenSignal("MM", "GB", priorProbs, prior);
		double probGBGivenGB = 1 - probMMGivenGB;
		
		double strategyGBGivenMM = 1 - strategyMMGivenMM;
		double strategyGBGivenGB = 1 - strategyMMGivenGB;
		
		if (signal.equals("MM")) {
			if (refReport.equals("MM")) {
				return probMMGivenMM * strategyMMGivenMM + probGBGivenMM * strategyMMGivenGB;
			} else if (refReport.equals("GB")) {
				return probMMGivenMM * strategyGBGivenMM + probGBGivenMM * strategyGBGivenGB;				
			}
		} else if (signal.equals("GB")) {
			if (refReport.equals("MM")) {
				return probMMGivenGB * strategyMMGivenMM + probGBGivenGB * strategyMMGivenGB;
			} else if (refReport.equals("GB")) {
				return probMMGivenGB * strategyGBGivenMM + probGBGivenGB * strategyGBGivenGB;
			}
		}
		return -1;
	}

	public static double getProbRefReportsGivenSignalAndStrategy(
			int numMM, int numGB, String signal, 
			double strategyMMGivenMM, double strategyMMGivenGB, 
			double[] priorProbs, List<Map<String, Double>> prior) {
		
		double probMMGivenMM = AnalysisUtils.getProbSignalGivenSignal("MM", "MM", priorProbs, prior);
		double probGBGivenMM = 1 - probMMGivenMM;
		double probMMGivenGB = AnalysisUtils.getProbSignalGivenSignal("MM", "GB", priorProbs, prior);
		double probGBGivenGB = 1 - probMMGivenGB;
		
		double strategyGBGivenMM = 1 - strategyMMGivenMM;
		double strategyGBGivenGB = 1 - strategyMMGivenGB;
		
		if (signal.equals("MM")) {
			return ArithmeticUtils.binomialCoefficient(3, numMM)
					* Math.pow((probMMGivenMM * strategyMMGivenMM + probGBGivenMM * strategyMMGivenGB), numMM)
					* Math.pow((probMMGivenMM * strategyGBGivenMM + probGBGivenMM * strategyGBGivenGB), numGB);
		} else if (signal.equals("GB")) {
			return ArithmeticUtils.binomialCoefficient(3, numMM)
					* Math.pow((probMMGivenGB * strategyMMGivenMM + probGBGivenGB * strategyMMGivenGB), numMM)
					* Math.pow((probMMGivenGB * strategyGBGivenMM + probGBGivenGB * strategyGBGivenGB), numGB);
		}
		return -1;
	}

	public static double getPayment(String treatment, String myReport, Object refInfo) {
		if (treatment.equals("prior2-basic"))
			return AnalysisUtils.getPaymentTreatmentBasic(myReport, (String) refInfo);
		else if (treatment.equals("prior2-outputagreement"))
			return AnalysisUtils.getPaymentTreatmentOutputAgreement(myReport,
					(String) refInfo);
		else if (treatment.equals("prior2-uniquetruthful") || treatment.equals("prior2-symmlowpay")) {
			int numMMInRefReports = (int) refInfo;
			if (treatment.equals("prior2-uniquetruthful"))
				return AnalysisUtils.getPaymentTreatmentUniqueTruthful(myReport, numMMInRefReports);
			else if (treatment.equals("prior2-symmlowpay"))
				return AnalysisUtils.getPaymentTreatmentSymmLowPay(myReport, numMMInRefReports);
		}

		return -1;
	}

	public static double getPaymentTreatmentBasic(String myReport, String refReport) {
		if (myReport.equals("MM") && refReport.equals("MM"))
			return 1.5;
		else if (myReport.equals("MM") && refReport.equals("GB"))
			return 0.1;
		else if (myReport.equals("GB") && refReport.equals("MM"))
			return 0.3;
		else if (myReport.equals("GB") && refReport.equals("GB"))
			return 1.2;
		return -1;
	}

	public static double getPaymentTreatmentOutputAgreement(String myReport, String refReport) {
		if (myReport.equals(refReport))
			return 1.5;
		else 
			return 0.1;
	}

	public static double getPaymentTreatmentUniqueTruthful(String myReport, int numMMOtherReports) {
		if (myReport.equals("MM")) {
			switch (numMMOtherReports) {
			case 0:
				return 0.90;
			case 1:
				return 0.10;
			case 2:
				return 1.50;
			case 3:
				return 0.80;
			}
	
		} else {
			switch (numMMOtherReports) {
			case 0:
				return 0.80;
			case 1:
				return 1.50;
			case 2:
				return 0.10;
			case 3:
				return 0.90;
			}
		}
		return -1;
	}

	public static double getPaymentTreatmentSymmLowPay(String myReport, int numMMOtherReports) {
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

}
