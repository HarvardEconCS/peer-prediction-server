package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;

import org.apache.commons.math3.util.ArithmeticUtils;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;

import com.google.common.collect.ImmutableMap;
import com.google.gson.Gson;

public class Utils {

	static final double epsConstruct = 0.01;
	static final double eps = 1e-6;
	public static final String[] signalList = new String[] { "MM", "GB" };

	public static final Gson gson = new Gson();

	static int em_N;
	static int em_K;
	static double[] em_pi;
	static Strategy[] em_strategies;
	static double em_likelihood;

	// Use an even prior to fit shit
	static double[] signalPrior = new double[] { 0.5, 0.5 };
	static Random rand = new Random();

	public static int getNumMMInRefReports(List<String> refReports) {
		int numMM = 0;
		for (String report : refReports) {
			if (report.equals("MM"))
				numMM++;
		}
		return numMM;
	}

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

		System.out.printf(
				"\nSolving for symmetric mixed strategy equilibrium.  %s\n",
				rule);

		double[] priorProbs = new double[] { 0.5, 0.5 };
		List<Map<String, Double>> prior = new ArrayList<Map<String, Double>>();
		prior.add(ImmutableMap.of("MM", 0.2, "GB", 0.8));
		prior.add(ImmutableMap.of("MM", 0.7, "GB", 0.3));

		double minDiffGB = Double.POSITIVE_INFINITY;
		double minDiffMM = Double.POSITIVE_INFINITY;
		double[] strategy = new double[] { 0.0, 0.0 };

		double unit = 0.001;
		int numUnit = (int) (1 / unit);
		for (int i = 1; i < numUnit; i++) {
			for (int j = 1; j < numUnit; j++) {
				double strategyMMGivenMM = unit * i;
				double strategyMMGivenGB = unit * j;

				double payoffMMSignalMMReport = Utils
						.getMixedPayoffForSignalAndReport3Players(rule,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "MM", "MM");
				double payoffMMSignalGBReport = Utils
						.getMixedPayoffForSignalAndReport3Players(rule,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "MM", "GB");
				double diffMMSignal = Math.abs(payoffMMSignalMMReport
						- payoffMMSignalGBReport);

				double payoffForGBSignalMMReport = Utils
						.getMixedPayoffForSignalAndReport3Players(rule,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "GB", "MM");
				double payoffForGBSignalGBReport = Utils
						.getMixedPayoffForSignalAndReport3Players(rule,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "GB", "GB");
				double diffGBSignal = Math.abs(payoffForGBSignalMMReport
						- payoffForGBSignalGBReport);

				if (diffGBSignal < minDiffGB && diffMMSignal < minDiffMM) {
					minDiffGB = diffGBSignal;
					minDiffMM = diffMMSignal;
					strategy[0] = strategyMMGivenMM;
					strategy[1] = strategyMMGivenGB;
				}
			}
		}

		System.out.printf("Close to mixed strategy equilibrium is %s\n",
				Arrays.toString(strategy));
		System.out.printf(
				"Best diff is %.4f for MM signal and %.4f for GB signal\n",
				minDiffMM, minDiffGB);
	}

	public static void getSymmMixedStrEq4Players(String treatment) {

		System.out.printf(
				"\nSolving for symmetric mixed strategy equilibrium.  %s\n",
				treatment);

		double[] priorProbs = new double[] { 0.5, 0.5 };
		List<Map<String, Double>> prior = new ArrayList<Map<String, Double>>();
		prior.add(ImmutableMap.of("MM", 0.2, "GB", 0.8));
		prior.add(ImmutableMap.of("MM", 0.7, "GB", 0.3));

		double minDiffGB = Double.POSITIVE_INFINITY;
		double minDiffMM = Double.POSITIVE_INFINITY;
		double[] strategy = new double[] { 0.0, 0.0 };

		double unit = 0.001;
		int numUnit = (int) (1 / unit);
		for (int i = 1; i < numUnit; i++) {
			for (int j = 1; j < numUnit; j++) {
				double strategyMMGivenMM = unit * i;
				double strategyMMGivenGB = unit * j;

				double payoffMMSignalMMReport = Utils
						.getMixedPayoffForSignalAndReport4Players(treatment,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "MM", "MM");
				double payoffMMSignalGBReport = Utils
						.getMixedPayoffForSignalAndReport4Players(treatment,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "MM", "GB");
				double diffMMSignal = Math.abs(payoffMMSignalMMReport
						- payoffMMSignalGBReport);

				double payoffForGBSignalMMReport = Utils
						.getMixedPayoffForSignalAndReport4Players(treatment,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "GB", "MM");
				double payoffForGBSignalGBReport = Utils
						.getMixedPayoffForSignalAndReport4Players(treatment,
								priorProbs, prior, strategyMMGivenMM,
								strategyMMGivenGB, "GB", "GB");
				double diffGBSignal = Math.abs(payoffForGBSignalMMReport
						- payoffForGBSignalGBReport);

				if (diffGBSignal < minDiffGB && diffMMSignal < minDiffMM) {
					minDiffGB = diffGBSignal;
					minDiffMM = diffMMSignal;
					strategy[0] = strategyMMGivenMM;
					strategy[1] = strategyMMGivenGB;
				}
			}
		}

		System.out.printf("Close to mixed strategy equilibrium is %s\n",
				Arrays.toString(strategy));
		System.out.printf(
				"Best diff is %.4f for MM signal and %.4f for GB signal\n",
				minDiffMM, minDiffGB);
	}

	/**
	 * EM Algorithms
	 * 
	 * @param signalReportPairs
	 */
	public static void runEMAlgorithm(
			List<List<Pair<String, String>>> signalReportPairs) {

		int N = signalReportPairs.size();

		int K = Utils.em_K;

		if (Utils.em_pi == null) {
			em_pi = new double[K];
			for (int k = 0; k < K; k++) {
				em_pi[k] = 1.0 / K;
			}
		}

		if (Utils.em_strategies == null) {
			em_strategies = new Strategy[K];
			for (int k = 0; k < K; k++) {
				em_strategies[k] = Strategy.getRandomStrategy();
			}
		}

		em_likelihood = Utils.getLogLikelihood(signalReportPairs, em_pi,
				em_strategies);

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

			double likelihood_temp = Utils.getLogLikelihood(signalReportPairs,
					pi_temp, strategies_temp);

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
			double value = list.get(i + 1).doubleValue()
					- list.get(i).doubleValue();
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

		double total0 = Utils.getProbWorldGivenSignal(priorProbs, prior, 0,
				signal)
				* Math.pow(prior.get(0).get("MM"), numMM)
				* Math.pow(prior.get(0).get("GB"), numGB)
				* ArithmeticUtils.binomialCoefficient(3, numMM);
		double total1 = Utils.getProbWorldGivenSignal(priorProbs, prior, 1,
				signal)
				* Math.pow(prior.get(1).get("MM"), numMM)
				* Math.pow(prior.get(1).get("GB"), numGB)
				* ArithmeticUtils.binomialCoefficient(3, numMM);
		return total0 + total1;
	}

	public static double getProbSignalGivenSignal(String resultSignal,
			String givenSignal, double[] priorProbs,
			List<Map<String, Double>> prior) {
		double total0 = Utils.getProbWorldGivenSignal(priorProbs, prior, 0,
				givenSignal) * prior.get(0).get(resultSignal);
		double total1 = Utils.getProbWorldGivenSignal(priorProbs, prior, 1,
				givenSignal) * prior.get(1).get(resultSignal);
		return total0 + total1;
	}

	public static double getProbWorldGivenSignal(double[] priorProbs,
			List<Map<String, Double>> prior, int i, String signal) {
		double probSignalAndWorld = priorProbs[i] * prior.get(i).get(signal);
		double probSignal = priorProbs[0] * prior.get(0).get(signal)
				+ priorProbs[1] * prior.get(1).get(signal);
		return probSignalAndWorld / probSignal;
	}

	public static double getTruthfulPayoff(String treatment,
			double[] priorProbs, List<Map<String, Double>> prior) {

		double probMM = Utils.getProbMM(priorProbs, prior);
		double probGB = 1 - probMM;

		double prob3MMGivenMM = Utils.getProbSignalsGivenSignal(3, 0, "MM",
				priorProbs, prior);
		double prob2MMGivenMM = Utils.getProbSignalsGivenSignal(2, 1, "MM",
				priorProbs, prior);
		double prob1MMGivenMM = Utils.getProbSignalsGivenSignal(1, 2, "MM",
				priorProbs, prior);
		double prob0MMGivenMM = Utils.getProbSignalsGivenSignal(0, 3, "MM",
				priorProbs, prior);

		double prob3MMGivenGB = Utils.getProbSignalsGivenSignal(3, 0, "GB",
				priorProbs, prior);
		double prob2MMGivenGB = Utils.getProbSignalsGivenSignal(2, 1, "GB",
				priorProbs, prior);
		double prob1MMGivenGB = Utils.getProbSignalsGivenSignal(1, 2, "GB",
				priorProbs, prior);
		double prob0MMGivenGB = Utils.getProbSignalsGivenSignal(0, 3, "GB",
				priorProbs, prior);

		return probMM
				* (prob3MMGivenMM * Utils.getPayment(treatment, "MM", 3)
						+ prob2MMGivenMM * Utils.getPayment(treatment, "MM", 2)
						+ prob1MMGivenMM * Utils.getPayment(treatment, "MM", 1) + prob0MMGivenMM
						* Utils.getPayment(treatment, "MM", 0))
				+ probGB
				* (prob3MMGivenGB * Utils.getPayment(treatment, "GB", 3)
						+ prob2MMGivenGB * Utils.getPayment(treatment, "GB", 2)
						+ prob1MMGivenGB * Utils.getPayment(treatment, "GB", 1) + prob0MMGivenGB
						* Utils.getPayment(treatment, "GB", 0));

	}

	private static double getMixedPayoffForSignalAndReport3Players(
			String treatment, double[] priorProbs,
			List<Map<String, Double>> prior, double strategyMMGivenMM,
			double strategyMMGivenGB, String signal, String report) {

		double probMMRefReportGivenMM = Utils
				.getProbRefReportGivenSignalAndStrategy("MM", "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double probGBRefReportGivenMM = Utils
				.getProbRefReportGivenSignalAndStrategy("GB", "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		double probMMRefReportGivenGB = Utils
				.getProbRefReportGivenSignalAndStrategy("MM", "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double probGBRefReportGivenGB = Utils
				.getProbRefReportGivenSignalAndStrategy("GB", "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		if (signal.equals("MM") && report.equals("MM")) {
			return probMMRefReportGivenMM
					* Utils.getPayment(treatment, "MM", "MM")
					+ probGBRefReportGivenMM
					* Utils.getPayment(treatment, "MM", "GB");
		} else if (signal.equals("MM") && report.equals("GB")) {
			return probMMRefReportGivenMM
					* Utils.getPayment(treatment, "GB", "MM")
					+ probGBRefReportGivenMM
					* Utils.getPayment(treatment, "GB", "GB");
		} else if (signal.equals("GB") && report.equals("MM")) {
			return probMMRefReportGivenGB
					* Utils.getPayment(treatment, "MM", "MM")
					+ probGBRefReportGivenGB
					* Utils.getPayment(treatment, "MM", "GB");
		} else if (signal.equals("GB") && report.equals("GB")) {
			return probMMRefReportGivenGB
					* Utils.getPayment(treatment, "GB", "MM")
					+ probGBRefReportGivenGB
					* Utils.getPayment(treatment, "GB", "GB");

		}
		return -1;
	}

	public static double getMixedPayoffForSignalAndReport4Players(
			String treatment, double[] priorProbs,
			List<Map<String, Double>> prior, double strategyMMGivenMM,
			double strategyMMGivenGB, String signal, String report) {

		double prob3MM0GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(3, 0, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(2, 1, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(1, 2, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(0, 3, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		double prob3MM0GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(3, 0, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(2, 1, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(1, 2, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(0, 3, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		if (signal.equals("MM") && report.equals("MM")) {
			return prob3MM0GBRefReportsGivenMM
					* Utils.getPayment(treatment, "MM", 3)
					+ prob2MM1GBRefReportsGivenMM
					* Utils.getPayment(treatment, "MM", 2)
					+ prob1MM2GBRefReportsGivenMM
					* Utils.getPayment(treatment, "MM", 1)
					+ prob0MM3GBRefReportsGivenMM
					* Utils.getPayment(treatment, "MM", 0);
		} else if (signal.equals("MM") && report.equals("GB")) {
			return prob3MM0GBRefReportsGivenMM
					* Utils.getPayment(treatment, "GB", 3)
					+ prob2MM1GBRefReportsGivenMM
					* Utils.getPayment(treatment, "GB", 2)
					+ prob1MM2GBRefReportsGivenMM
					* Utils.getPayment(treatment, "GB", 1)
					+ prob0MM3GBRefReportsGivenMM
					* Utils.getPayment(treatment, "GB", 0);
		} else if (signal.equals("GB") && report.equals("MM")) {
			return prob3MM0GBRefReportsGivenGB
					* Utils.getPayment(treatment, "MM", 3)
					+ prob2MM1GBRefReportsGivenGB
					* Utils.getPayment(treatment, "MM", 2)
					+ prob1MM2GBRefReportsGivenGB
					* Utils.getPayment(treatment, "MM", 1)
					+ prob0MM3GBRefReportsGivenGB
					* Utils.getPayment(treatment, "MM", 0);
		} else if (signal.equals("GB") && report.equals("GB")) {
			return prob3MM0GBRefReportsGivenGB
					* Utils.getPayment(treatment, "GB", 3)
					+ prob2MM1GBRefReportsGivenGB
					* Utils.getPayment(treatment, "GB", 2)
					+ prob1MM2GBRefReportsGivenGB
					* Utils.getPayment(treatment, "GB", 1)
					+ prob0MM3GBRefReportsGivenGB
					* Utils.getPayment(treatment, "GB", 0);
		}
		return -1;
	}

	public static double getMixedPayoff(String treatment, double[] priorProbs,
			List<Map<String, Double>> prior, double strategyMMGivenMM,
			double strategyMMGivenGB) {

		// System.out.printf("%.2f, %.2f", strategyMMGivenMM,
		// strategyMMGivenGB);

		double prob3MM0GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(3, 0, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(2, 1, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(1, 2, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenMM = Utils
				.getProbRefReportsGivenSignalAndStrategy(0, 3, "MM",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		double prob3MM0GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(3, 0, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob2MM1GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(2, 1, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob1MM2GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(1, 2, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);
		double prob0MM3GBRefReportsGivenGB = Utils
				.getProbRefReportsGivenSignalAndStrategy(0, 3, "GB",
						strategyMMGivenMM, strategyMMGivenGB, priorProbs, prior);

		// System.out.printf("%.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f, %.2f",
		// prob3MM0GBRefReportsGivenMM, prob2MM1GBRefReportsGivenMM,
		// prob1MM2GBRefReportsGivenMM, prob0MM3GBRefReportsGivenMM,
		// prob3MM0GBRefReportsGivenGB, prob2MM1GBRefReportsGivenGB,
		// prob1MM2GBRefReportsGivenGB, prob0MM3GBRefReportsGivenGB);

		double probMM = Utils.getProbMM(priorProbs, prior);
		double probGB = 1 - probMM;

		return probMM
				* strategyMMGivenMM
				* (prob3MM0GBRefReportsGivenMM
						* Utils.getPayment(treatment, "MM", 3)
						+ prob2MM1GBRefReportsGivenMM
						* Utils.getPayment(treatment, "MM", 2)
						+ prob1MM2GBRefReportsGivenMM
						* Utils.getPayment(treatment, "MM", 1) + prob0MM3GBRefReportsGivenMM
						* Utils.getPayment(treatment, "MM", 0))
				+ probGB
				* strategyMMGivenGB
				* (prob3MM0GBRefReportsGivenGB
						* Utils.getPayment(treatment, "MM", 3)
						+ prob2MM1GBRefReportsGivenGB
						* Utils.getPayment(treatment, "MM", 2)
						+ prob1MM2GBRefReportsGivenGB
						* Utils.getPayment(treatment, "MM", 1) + prob0MM3GBRefReportsGivenGB
						* Utils.getPayment(treatment, "MM", 0))
				+ probMM
				* (1 - strategyMMGivenMM)
				* (prob3MM0GBRefReportsGivenMM
						* Utils.getPayment(treatment, "GB", 3)
						+ prob2MM1GBRefReportsGivenMM
						* Utils.getPayment(treatment, "GB", 2)
						+ prob1MM2GBRefReportsGivenMM
						* Utils.getPayment(treatment, "GB", 1) + prob0MM3GBRefReportsGivenMM
						* Utils.getPayment(treatment, "GB", 0))
				+ probGB
				* (1 - strategyMMGivenGB)
				* (prob3MM0GBRefReportsGivenGB
						* Utils.getPayment(treatment, "GB", 3)
						+ prob2MM1GBRefReportsGivenGB
						* Utils.getPayment(treatment, "GB", 2)
						+ prob1MM2GBRefReportsGivenGB
						* Utils.getPayment(treatment, "GB", 1) + prob0MM3GBRefReportsGivenGB
						* Utils.getPayment(treatment, "GB", 0));
	}

	public static double getProbRefReportGivenSignalAndStrategy(
			String refReport, String signal, double strategyMMGivenMM,
			double strategyMMGivenGB, double[] priorProbs,
			List<Map<String, Double>> prior) {

		double probMMGivenMM = Utils.getProbSignalGivenSignal("MM", "MM",
				priorProbs, prior);
		double probGBGivenMM = 1 - probMMGivenMM;
		double probMMGivenGB = Utils.getProbSignalGivenSignal("MM", "GB",
				priorProbs, prior);
		double probGBGivenGB = 1 - probMMGivenGB;

		double strategyGBGivenMM = 1 - strategyMMGivenMM;
		double strategyGBGivenGB = 1 - strategyMMGivenGB;

		if (signal.equals("MM")) {
			if (refReport.equals("MM")) {
				return probMMGivenMM * strategyMMGivenMM + probGBGivenMM
						* strategyMMGivenGB;
			} else if (refReport.equals("GB")) {
				return probMMGivenMM * strategyGBGivenMM + probGBGivenMM
						* strategyGBGivenGB;
			}
		} else if (signal.equals("GB")) {
			if (refReport.equals("MM")) {
				return probMMGivenGB * strategyMMGivenMM + probGBGivenGB
						* strategyMMGivenGB;
			} else if (refReport.equals("GB")) {
				return probMMGivenGB * strategyGBGivenMM + probGBGivenGB
						* strategyGBGivenGB;
			}
		}
		return -1;
	}

	public static double getProbRefReportsGivenSignalAndStrategy(int numMM,
			int numGB, String signal, double strategyMMGivenMM,
			double strategyMMGivenGB, double[] priorProbs,
			List<Map<String, Double>> prior) {

		double probMMGivenMM = Utils.getProbSignalGivenSignal("MM", "MM",
				priorProbs, prior);
		double probGBGivenMM = 1 - probMMGivenMM;
		double probMMGivenGB = Utils.getProbSignalGivenSignal("MM", "GB",
				priorProbs, prior);
		double probGBGivenGB = 1 - probMMGivenGB;

		double strategyGBGivenMM = 1 - strategyMMGivenMM;
		double strategyGBGivenGB = 1 - strategyMMGivenGB;

		if (signal.equals("MM")) {
			return ArithmeticUtils.binomialCoefficient(3, numMM)
					* Math.pow(
							(probMMGivenMM * strategyMMGivenMM + probGBGivenMM
									* strategyMMGivenGB), numMM)
					* Math.pow(
							(probMMGivenMM * strategyGBGivenMM + probGBGivenMM
									* strategyGBGivenGB), numGB);
		} else if (signal.equals("GB")) {
			return ArithmeticUtils.binomialCoefficient(3, numMM)
					* Math.pow(
							(probMMGivenGB * strategyMMGivenMM + probGBGivenGB
									* strategyMMGivenGB), numMM)
					* Math.pow(
							(probMMGivenGB * strategyGBGivenMM + probGBGivenGB
									* strategyGBGivenGB), numGB);
		}
		return -1;
	}

	public static double getPayment(String treatment, String myReport,
			Object refInfo) {
		if (treatment.equals("prior2-basic"))
			return Utils.getPaymentTreatmentBasic(myReport, (String) refInfo);
		else if (treatment.equals("prior2-outputagreement"))
			return Utils.getPaymentTreatmentOutputAgreement(myReport,
					(String) refInfo);
		else if (treatment.equals("prior2-uniquetruthful")
				|| treatment.equals("prior2-symmlowpay")) {
			int numMMInRefReports = (int) refInfo;
			if (treatment.equals("prior2-uniquetruthful"))
				return Utils.getPaymentTreatmentUniqueTruthful(myReport,
						numMMInRefReports);
			else if (treatment.equals("prior2-symmlowpay"))
				return Utils.getPaymentTreatmentSymmLowPay(myReport,
						numMMInRefReports);
		}

		return -1;
	}

	public static double getPaymentTreatmentBasic(String myReport,
			String refReport) {
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

	public static double getPaymentTreatmentOutputAgreement(String myReport,
			String refReport) {
		if (myReport.equals(refReport))
			return 1.5;
		else
			return 0.1;
	}

	public static double getPaymentTreatmentUniqueTruthful(String myReport,
			int numMMOtherReports) {
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

	public static double getPaymentTreatmentSymmLowPay(String myReport,
			int numMMOtherReports) {
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

	public static boolean isGBStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.GB);
		SigActObservation<CandySignal, CandyReport> gbGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmGB) > 0.8 && opdf.probability(gbGB) > 0.8;
	}

	public static boolean isBetterGBStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1,
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf2) {
		SigActObservation<CandySignal, CandyReport> mmGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.GB);
		SigActObservation<CandySignal, CandyReport> gbGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.GB);
		return opdf1.probability(mmGB) > opdf2.probability(mmGB)
				&& opdf1.probability(gbGB) > opdf2.probability(gbGB);
	}

	public static boolean isMMStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.MM);
		return opdf.probability(mmMM) > 0.8 && opdf.probability(gbMM) > 0.8;
	}

	public static boolean isBetterMMStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1,
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf2) {

		SigActObservation<CandySignal, CandyReport> mmMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.MM);
		return opdf1.probability(mmMM) > opdf2.probability(mmMM)
				&& opdf1.probability(gbMM) > opdf2.probability(gbMM);
	}

	public static boolean isTruthfulStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf) {
		SigActObservation<CandySignal, CandyReport> mmMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.GB);
		return opdf.probability(mmMM) > 0.8 && opdf.probability(gbGB) > 0.8;
	}

	public static boolean isBetterTruthfulStrategy(
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1,
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf2) {

		SigActObservation<CandySignal, CandyReport> mmMM = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.MM, CandyReport.MM);
		SigActObservation<CandySignal, CandyReport> gbGB = new SigActObservation<CandySignal, CandyReport>(
				CandySignal.GB, CandyReport.GB);
		return opdf1.probability(mmMM) > opdf2.probability(mmMM)
				&& opdf1.probability(gbGB) > opdf2.probability(gbGB);
	}

	public static void normalizeDist(double[] actionDist) {
		double sum = 0.0;
		for (int x = 0; x < actionDist.length; x++) {
			sum += actionDist[x];
		}
		if (sum == 0)
			return;
		for (int x = 0; x < actionDist.length; x++) {
			actionDist[x] = actionDist[x] / sum;
		}
	}

	public static double calcMMProb(double lambda, double attrMMReport,
			double attrGBPayoff) {
		double mmProb = Math.pow(Math.E, lambda * attrMMReport)
				/ (Math.pow(Math.E, lambda * attrMMReport) + Math.pow(Math.E,
						lambda * attrGBPayoff));
		return mmProb;
	}

	/**
	 * Get number of reports that are same as the given report in the result, excluding a player
	 * @param roundResult
	 * @param givenReport
	 * @param excludePlayerId
	 * @return
	 */
	public static int getNumOfGivenReport(
			Map<String, Map<String, Object>> roundResult, String givenReport,
			String excludePlayerId) {

		int num = 0;
		for (String playerId : roundResult.keySet()) {
			if (!excludePlayerId.equals(playerId)) {
				String report = (String) roundResult.get(playerId)
						.get("report");
				if (report.equals(givenReport)) {
					num++;
				}
			}
		}
		return num;
	}

	public static int chooseRefPlayer(int currPlayerIndex) {
		int shift = Utils.rand.nextInt(2);
		if (shift >= currPlayerIndex)
			return shift + 1;
		else
			return shift;

	}

	public static int selectByBinaryDist(double firstProb) {
		double next = Utils.rand.nextDouble();
		if (next >= firstProb)
			return 1;
		return 0;
	}

	public static Hmm<SigActObservation<CandySignal, CandyReport>> getRandomHmm(
			int numStates) {

		double[] pi = getRandomVec(numStates);

		double[][] a = new double[numStates][numStates];
		for (int i = 0; i < a.length; i++) {
			a[i] = getRandomVec(numStates);
		}

		List<OpdfStrategy<CandySignal, CandyReport>> opdfs = new ArrayList<OpdfStrategy<CandySignal, CandyReport>>();

		for (int i = 0; i < numStates; i++) {
			double[][] probs = new double[][] { getRandomVec(2),
					getRandomVec(2) };
			opdfs.add(Utils.createOpdf(probs));
		}

		return new Hmm<SigActObservation<CandySignal, CandyReport>>(pi, a,
				opdfs);
	}

	public static OpdfStrategy<CandySignal, CandyReport> createOpdf(
			double[][] probs) {
		return new OpdfStrategy<CandySignal, CandyReport>(CandySignal.class,
				CandyReport.class, Utils.signalPrior, probs);
	}

	public static double getExpectedPayoff(String treatment, String myReport,
			Map<String, Double> oppPopStrategy) {

		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			return getPayment(treatment, myReport, "MM")
					* oppPopStrategy.get("MM")
					* oppPopStrategy.get("MM")

					+ (getPayment(treatment, myReport, "MM") * 0.5 + getPayment(
							treatment, myReport, "GB") * 0.5) * 2
					* oppPopStrategy.get("MM") * oppPopStrategy.get("GB")

					+ getPayment(treatment, myReport, "GB")
					* oppPopStrategy.get("GB") * oppPopStrategy.get("GB");

		} else if (treatment.equals("prior2-uniquetruthful")) {
			return getPayment(treatment, myReport, 0)
					* oppPopStrategy.get("GB")
					* oppPopStrategy.get("GB")
					* oppPopStrategy.get("GB")

					+ getPayment(treatment, myReport, 1)
					* (3 * oppPopStrategy.get("MM") * oppPopStrategy.get("GB") * oppPopStrategy
							.get("GB"))

					+ getPayment(treatment, myReport, 2)
					* (3 * oppPopStrategy.get("MM") * oppPopStrategy.get("MM") * oppPopStrategy
							.get("GB"))

					+ getPayment(treatment, myReport, 3)
					* oppPopStrategy.get("MM") * oppPopStrategy.get("MM")
					* oppPopStrategy.get("MM");
		}

		return -1;
	}

	public static String getBestResponse(String treatment,
			Map<String, Double> oppPopStrategy) {

		String myReport;

		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			double payoffMM = getExpectedPayoff(treatment, "MM", oppPopStrategy);
			double payoffGB = getExpectedPayoff(treatment, "GB", oppPopStrategy);

			if ((payoffMM - payoffGB) > eps)
				myReport = "MM";
			else
				myReport = "GB";
			return myReport;

		} else if (treatment.equals("prior2-uniquetruthful")) {
			double payoffMM = getExpectedPayoff(treatment, "MM", oppPopStrategy);
			double payoffGB = getExpectedPayoff(treatment, "GB", oppPopStrategy);

			if ((payoffMM - payoffGB) > eps)
				myReport = "MM";
			else
				myReport = "GB";
			return myReport;
		}

		return null;
	}

	public static double getNumMMReports(int roundStart, int roundEnd,
			Game game, String excludePlayerId) {
		int countMM = 0;
		for (String playerId : game.playerHitIds) {
			if (playerId.equals(excludePlayerId))
				continue;
			for (int i = roundStart; i <= roundEnd; i++) {
				String report = game.rounds.get(i).getReport(playerId);
				if (report.equals("MM"))
					countMM++;
			}
		}
		return countMM;
	}

}
