package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;
import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;

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

	/**
	 * roundStart inclusive, roundEnd exclusive
	 * @param game
	 * @param excludePlayerId
	 * @param roundStart
	 * @param roundEnd
	 * @return
	 */
	public static int getNumMMReports(Game game, String excludePlayerId,
			int roundStart, int roundEnd) {
		int countMM = 0;
		for (String playerId : game.playerHitIds) {
			if (playerId.equals(excludePlayerId))
				continue;
			for (int i = roundStart; i < roundEnd; i++) {
				String report = game.rounds.get(i).getReport(playerId);
				if (report.equals("MM"))
					countMM++;
			}
		}
		return countMM;
	}

	public static String getOtherReport(String report) {
		if (report.equals("MM"))
			return "GB";
		else
			return "MM";
	}

	public static void printParams(Map<String, Object> params) {
		List<String> keys = new ArrayList<String>();
		keys.addAll(params.keySet());
		Collections.sort(keys);
		for (String key : keys) {
			if (key.equals("isAbs")) continue;
			System.out.printf("%s=%.3f, ", key, params.get(key));
		}
		System.out.println();
	}

}
