package edu.harvard.econcs.peerprediction.analysis;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import java.text.ParseException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.andrewmao.models.games.BWToleranceLearner;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;

import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;

import com.google.common.collect.ImmutableMap;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;

public class LogReader {

	static String dbUrl = "jdbc:mysql://localhost/peerprediction";
	static String dbClass = "com.mysql.jdbc.Driver";
	static String setId = "vary-payment";
	static String treatment = "prior2-basic";
	// static String treatment = "prior2-outputagreement";
	// static String treatment = "prior2-uniquetruthful";
	// static String treatment = "prior2-symmlowpay";
	// static String treatment = "prior2-constant";

	static final String rootDir = "/Users/alicexigao/Dropbox/peer-prediction/data/"
			+ treatment + "/";

	static Experiment expSet;

	// For HMM estimation
	static int numStrategies = -1;
	static String[] strategyNames = null;
	static int numRestarts = 10;
	static double tol = 0.02;
	static Hmm<SigActObservation<CandySignal, CandyReport>> origHmm = null;
	static Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm = null;
	static int mmState = -1;
	static int gbState = -1;
	static int truthfulState = -1;
	static int mixedState = -1;
	static int mixed2State = -1;

	static String[] signalList = new String[] { "MM", "GB" };

	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();

		// Parsing game log and exit survey, and print info
		parseLog();
		writePlayerComments();
		printInfo();

		// Statistics of raw data
		writeRawDataToFile();
		graphRawData();
		writeAvgBonus();

		// HMM analysis
		learnHMM();
		setStrategyNames();
		// writeStateSeq();
		// eqConvergenceHmm();
		// writeStrategyChangeHeatMap();

		// writeStrategyDistribution();
		// genPredictedStrategyChangeCode();
		// graphLogLikelihood();

		// Simple analysis
		// eqConvergenceSimpleMethod();

		estimateSFP();
		estimateRL();

		// Learning analysis
		// simulateSFP();
		// simulateSFPOneGame(1, 5, false);
		// simulateRL();
		// simulateRLOneGame(1, 10, true);
		// learningAnalysis("BR");
		// learningAnalysis("FP");
		// noRegretLearningAnalysis();

		// Analyze payoffs of different strategies

		// strategyChangeT1();
		// mixedStrategyPayoff("T3");
		// strategyPayoffAnalysis("prior2-uniquetruthful");
		// strategyPayoffAnalysis("prior2-symmlowpay");
		// AnalysisUtils.getSymmMixedStrEq3Players("T1");
		// AnalysisUtils.getSymmMixedStrEq4Players("T3");
		// AnalysisUtils.getSymmMixedStrEq4Players("T5");
	}

	public static void estimateSFP() {

		System.out
				.println("\n\nEstimating Parameters for Stochastic Fictitious Play");

		double discount; // discount factor
		double discountStart = 0;
		double discountEnd = 1;

		double lambda; // sensitivity parameter
		double lambdaStart = 1;
		double lambdaEnd = 20;

		boolean[] signalOpt = new boolean[] { true, false };

		double bestDiscount = 0;
		double bestLambda = 0;
		boolean bestSignalOpt = true;
		double bestLogLk = Double.NEGATIVE_INFINITY;

		for (discount = discountStart; discount <= discountEnd; discount += 0.05) {
			for (lambda = lambdaStart; lambda <= lambdaEnd; lambda++) {
				for (int i = 0; i < signalOpt.length; i++) {

					boolean considerSignal = signalOpt[i];

					double loglk = computeLogLkSFP(lambda, discount,
							considerSignal);

					if (loglk > bestLogLk) {
						bestDiscount = discount;
						bestLambda = lambda;
						bestSignalOpt = considerSignal;
						bestLogLk = loglk;
						// System.out.printf("l: %.2f, d: %.2f, s: %b, loglk=%.2f\n",
						// lambda, discount, considerSignal, loglk);
					}
				}
			}
		}

		System.out.printf(
				"Best parameters for SFP; l=%.2f, d=%.2f, s=%b, loglk=%.2f\n",
				bestLambda, bestDiscount, bestSignalOpt, bestLogLk);
	}

	/**
	 * Computer Log Likelihood for Stochastic Fictitious Play
	 */
	private static double computeLogLkSFP(double lambda, double discount,
			boolean considerSignal) {

		double loglk = 0;
		for (Game game : expSet.games) {

			double gameLogLk = 0;

			for (int i = 0; i < expSet.numRounds; i++) {

				if (i == 0) {

					gameLogLk += Math.log(Math.pow(0.5, expSet.numPlayers));

				} else {

					for (String currPlayerId : game.playerHitIds) {

						Map<String, Double> strategy = getStrategySFP(
								game.rounds, game.playerHitIds, i,
								currPlayerId, considerSignal, discount, lambda);
						String report = (String) game.rounds.get(i).result.get(
								currPlayerId).get("report");

						gameLogLk += Math.log(strategy.get(report));

					}
				}
			}
			loglk += gameLogLk;
		}
		return loglk;
	}

	private static Map<String, Double> getStrategySFP(List<Round> rounds,
			String[] playerHitIds, int currRound, String currPlayerId,
			boolean considerSignal, double discount, double lambda) {

		String signalRoundI = (String) rounds.get(currRound).result.get(
				currPlayerId).get("signal");

		// index i counts number of rounds to have i MM reports
		// for the other players.
		double[] dRoundCount = new double[expSet.numPlayers];
		double d = 1;

		for (int k = currRound - 1; k >= 0; k--) {

			Map<String, Map<String, Object>> roundResult = rounds.get(k).result;
			String signalRoundK = (String) roundResult.get(currPlayerId).get(
					"signal");

			if ((considerSignal && signalRoundK.equals(signalRoundI))
					|| (!considerSignal)) {
				int numMMForRound = Utils.getNumOfGivenReport("MM", playerHitIds,
						currPlayerId, roundResult);
				dRoundCount[numMMForRound] += d;
			}
			d *= discount;
		}

		Utils.normalizeDist(dRoundCount);
		double mmPayoff = getExpectedPayoffSFP("MM", dRoundCount);
		double gbPayoff = getExpectedPayoffSFP("GB", dRoundCount);

		double mmProb = Utils.calcMMProb(lambda, mmPayoff, gbPayoff);
		return ImmutableMap.of("MM", mmProb, "GB", 1 - mmProb);
	}

	public static void estimateRL() {

		System.out.println("\n\nEstimating Parameters for Reinforcement Learning");

		double discount; // discount factor
		double discountStart = 0;
		double discountEnd = 1;

		double lambda; // sensitivity parameter
		double lambdaStart = 1;
		double lambdaEnd = 20;

		boolean[] signalOpt = new boolean[] { true, false };

		double bestDiscount = 0;
		double bestLambda = 0;
		boolean bestSignalOpt = true;
		double bestLogLk = Double.NEGATIVE_INFINITY;

		for (discount = discountStart; discount <= discountEnd; discount += 0.05) {
			for (lambda = lambdaStart; lambda <= lambdaEnd; lambda++) {
				for (int i = 0; i < signalOpt.length; i++) {

					boolean considerSignal = signalOpt[i];

					double loglk = computeLogLkRL(considerSignal, discount,
							lambda);

					if (loglk > bestLogLk) {
						bestDiscount = discount;
						bestLambda = lambda;
						bestSignalOpt = considerSignal;
						bestLogLk = loglk;
						System.out.printf(
								"l: %.2f, d: %.2f, s: %b, loglk=%.2f\n",
								lambda, discount, considerSignal, loglk);
					}
				}
			}
		}

		System.out.printf(
				"Best parameters for RL; l=%.2f, d=%.2f, s=%b, loglk=%.2f\n",
				bestLambda, bestDiscount, bestSignalOpt, bestLogLk);
	}

	private static double computeLogLkRL(boolean considerSignal,
			double discount, double lambda) {

		double loglk = 0;
		for (Game game : expSet.games) {

			double gameLogLk = 0;

			for (int i = 0; i < expSet.numRounds; i++) {

				if (i == 0) {

					gameLogLk += Math.log(Math.pow(0.5, expSet.numPlayers));

				} else {

					// add likelihood for rewards
					for (String currPlayerId : game.playerHitIds) {

						Map<String, Map<String, Object>> result = game.rounds
								.get(i).result;

						String refPlayer = (String) result.get(currPlayerId)
								.get("refPlayer");
						String refReport = (String) result.get(refPlayer)
								.get("report");
						int numRefReport = Utils.getNumOfGivenReport(
								refReport, game.playerHitIds, currPlayerId,
								result);
						double rewardLogLk = Math.log(numRefReport * 1.0 / (expSet.numPlayers - 1));
						gameLogLk += rewardLogLk;
					}

					// add likelihood for reports
					for (String currPlayerId : game.playerHitIds) {

						Map<String, Double> strategy = getStrategyRL(
								game.rounds, game.playerHitIds, i,
								currPlayerId, considerSignal, discount, lambda);
						String report = (String) game.rounds.get(i).result.get(
								currPlayerId).get("report");

						gameLogLk += Math.log(strategy.get(report));

					}
				}
			}
			loglk += gameLogLk;
		}
		return loglk;
	}

	private static Map<String, Double> getStrategyRL(List<Round> rounds,
			String[] playerHitIds, int i, String currPlayerId,
			boolean considerSignal, double discount, double lambda) {

		String signalRoundI = (String) rounds.get(i).result.get(currPlayerId)
				.get("signal");

		Map<String, Double> payoffs = new HashMap<String, Double>();
		payoffs.put("MM", 0.0);
		payoffs.put("GB", 0.0);
		double d = 1;

		for (int k = i - 1; k >= 0; k--) {

			Map<String, Map<String, Object>> resultRoundK = rounds.get(k).result;

			Map<String, Object> currPlayerResult = resultRoundK
					.get(currPlayerId);
			String signal = (String) currPlayerResult.get("signal");

			if ((considerSignal && signal.equals(signalRoundI))
					|| (!considerSignal)) {

				String report = (String) currPlayerResult.get("report");
				double payoff = (double) currPlayerResult.get("reward");

				payoffs.put(report, payoffs.get(report) + payoff * d);
			}
			d *= discount;
		}

		double mmProb = Utils.calcMMProb(lambda, payoffs.get("MM"), payoffs.get("GB"));
		return ImmutableMap.of("MM", mmProb, "GB", 1 - mmProb);
	}

	public static double getExpectedPayoffSFP(String report, double[] roundDist) {
		double expPayoff = 0.0;
		for (int numMMReports = 0; numMMReports < roundDist.length; numMMReports++) {
			expPayoff += roundDist[numMMReports] * getExpectedPayoff(report, numMMReports);
		}
		return expPayoff;
	}

	public static double getExpectedPayoff(String report, int numOtherMMReports) {

		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			if (numOtherMMReports == 2) {
				return Utils.getPayment(treatment, report, "MM");
			} else if (numOtherMMReports == 0) {
				return Utils.getPayment(treatment, report, "GB");
			} else if (numOtherMMReports == 1) {
				return 0.5 * Utils.getPayment(treatment, report, "MM") 
					+ 0.5 * Utils.getPayment(treatment, report, "GB");
			}

		} else if (treatment.equals("prior2-uniquetruthful")
				|| treatment.equals("prior2-symmlowpay")) {
			return Utils.getPayment(treatment, report, numOtherMMReports);
		}

		return -1;
	}

	private static List<Round> simulateSFPOneGame(double discount,
			double lambda, boolean forSignal) {

		System.out.println("\n\nSimulating Stochastic Fictitious Play");

		List<Round> rounds = new ArrayList<Round>();

		String[] playerHitIds = new String[expSet.numPlayers];
		for (int index = 0; index < expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}

		for (int i = 0; i < expSet.numRounds; i++) {

			System.out.printf("Round %d\n", i);

			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(expSet.priorProbs[0]);
			r.chosenWorld = expSet.worlds.get(worldIndex);

			Map<String, Map<String, Object>> result = new HashMap<String, Map<String, Object>>();

			// Get signals and choose reports
			String[] signals = new String[expSet.numPlayers];
			String[] reports = new String[expSet.numPlayers];
			double[] mmProbs = new double[expSet.numPlayers];

			for (int j = 0; j < expSet.numPlayers; j++) {

				String currPlayerId = String.format("%d", j);

				// get signal
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
				signals[j] = signalList[signalIndex];

				// choose report
				if (i == 0) {

					// first round, choose reports randomly
					int reportIndex = Utils.selectByBinaryDist(0.5);
					reports[j] = signalList[reportIndex];

				} else {

					Map<String, Double> strategy = getStrategySFP(rounds,
							playerHitIds, i, currPlayerId, forSignal, discount,
							lambda);

					mmProbs[j] = strategy.get("MM").doubleValue();

					int reportIndex = Utils.selectByBinaryDist(mmProbs[j]);
					reports[j] = signalList[reportIndex];
				}

			}

			// determine payoffs
			int[] refPlayerIndices = new int[expSet.numPlayers];
			double[] payoffs = new double[expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);

			// save result
			for (int j = 0; j < expSet.numPlayers; j++) {

				Map<String, Object> info = new HashMap<String, Object>();

				info.put("signal", signals[j]);
				info.put("report", reports[j]);
				info.put("refPlayer", refPlayerIndices[j]);
				info.put("payoff", payoffs[j]);
				info.put("mmProb", mmProbs[j]);

				String id = String.format("%d", j);
				result.put(id, info);
			}

			r.result = result;
			System.out.printf("round %d, result: %s\n", i, result);
			rounds.add(r);
		}

		return rounds;
	}

	private static List<Round> simulateRLOneGame(boolean considerSignal,
			double discount, double lambda) {

		List<Round> rounds = new ArrayList<Round>();

		String[] playerHitIds = new String[expSet.numPlayers];
		for (int index = 0; index < expSet.numPlayers; index++) {
			playerHitIds[index] = String.format("%d", index);
		}

		for (int i = 0; i < expSet.numRounds; i++) {

			Round r = new Round();
			r.roundNum = i;
			int worldIndex = Utils.selectByBinaryDist(expSet.priorProbs[0]);
			r.chosenWorld = expSet.worlds.get(worldIndex);

			r.result = new HashMap<String, Map<String, Object>>();

			// get signals and choose reports
			String[] signals = new String[expSet.numPlayers];
			String[] reports = new String[expSet.numPlayers];
			double[] mmProbs = new double[expSet.numPlayers];
			for (int j = 0; j < expSet.numPlayers; j++) {

				String currPlayerId = String.format("%d", j);

				// get signal
				int signalIndex = Utils.selectByBinaryDist(r.chosenWorld.get("MM"));
				signals[j] = signalList[signalIndex];
				System.out.printf("signal=%s,", signals[j]);

				// choose report
				if (i == 0) {

					// first round, choose reports randomly
					int reportIndex = Utils.selectByBinaryDist(0.5);
					reports[j] = signalList[reportIndex];
					System.out.printf("report=%s,", reports[j]);

				} else {

					Map<String, Double> strategy = getStrategyRL(
							rounds, playerHitIds, i,
							currPlayerId, considerSignal, discount, lambda);
					mmProbs[j] = strategy.get("MM");
					int reportIndex = Utils.selectByBinaryDist(strategy.get("MM"));
					reports[j] = signalList[reportIndex];
				}

			}

			// determine payoffs
			int[] refPlayerIndices = new int[expSet.numPlayers];
			double[] payoffs = new double[expSet.numPlayers];
			determinePayoff(reports, refPlayerIndices, payoffs);

			// save result
			for (int j = 0; j < expSet.numPlayers; j++) {

				Map<String, Object> info = new HashMap<String, Object>();

				info.put("signal", signals[j]);
				info.put("report", reports[j]);
				info.put("refPlayer", refPlayerIndices[j]);
				info.put("payoff", payoffs[j]);
				info.put("mmProb", mmProbs[j]);

				String id = String.format("%d", j);
				r.result.put(id, info);
			}

			rounds.add(r);
		}
		return rounds;
	}

	private static double[] getAvgPayoffOneGame(List<Round> rounds) {
		double[] totalPayoff = new double[expSet.numPlayers];
		for (Round r : rounds) {
			for (int j = 0; j < expSet.numPlayers; j++) {
				String id = String.format("%d", j);
				totalPayoff[j] += (double) r.result.get(id).get("payoff");
			}
		}
		double[] averagePayoff = new double[expSet.numPlayers];
		for (int j = 0; j < expSet.numPlayers; j++) {
			averagePayoff[j] = totalPayoff[j] / rounds.size();
		}
		return averagePayoff;
	}

	private static void determinePayoff(String[] reports,
			int[] refPlayerIndices, double[] payoffs) {

		if (treatment.equals("prior2-basic")) {

			for (int j = 0; j < expSet.numPlayers; j++) {

				String myReport = reports[j];
				refPlayerIndices[j] = Utils.chooseRefPlayer(j);
				String refReport = reports[refPlayerIndices[j]];
				payoffs[j] = Utils.getPaymentTreatmentBasic(myReport,
						refReport);

			}

		} else if (treatment.equals("prior2-outputagreement")) {

			for (int j = 0; j < expSet.numPlayers; j++) {

				String myReport = reports[j];
				refPlayerIndices[j] = Utils.chooseRefPlayer(j);
				String refReport = reports[refPlayerIndices[j]];
				payoffs[j] = Utils.getPaymentTreatmentOutputAgreement(
						myReport, refReport);

			}
		}
	}

	public static void graphRawData() throws IOException {
		System.out.println("Graph raw data");

		int numPlayersPerGame = expSet.numPlayers;
		int totalNumPlayers = numPlayersPerGame * expSet.numGames;

		double[] numMMSignalsMMReports = new double[expSet.numRounds];
		double[] numMMSignalsGBReports = new double[expSet.numRounds];
		double[] numGBSignalsMMReports = new double[expSet.numRounds];
		double[] numGBSignalsGBReports = new double[expSet.numRounds];

		for (Game game : expSet.games) {
			int index = 0;
			for (Round round : game.rounds) {
				for (String hitId : game.playerHitIds) {
					if (round.getSignal(hitId).equals("MM")) {
						if (round.getReport(hitId).equals("MM")) {
							numMMSignalsMMReports[index]++;
						} else {
							numMMSignalsGBReports[index]++;
						}
					} else {
						if (round.getReport(hitId).equals("MM")) {
							numGBSignalsMMReports[index]++;
						} else {
							numGBSignalsGBReports[index]++;
						}
					}
				}
				index++;
			}
		}
		for (int i = 0; i < expSet.numRounds; i++) {
			numMMSignalsMMReports[i] = numMMSignalsMMReports[i]
					/ totalNumPlayers;
			numMMSignalsGBReports[i] = numMMSignalsGBReports[i]
					/ totalNumPlayers;
			numGBSignalsMMReports[i] = numGBSignalsMMReports[i]
					/ totalNumPlayers;
			numGBSignalsGBReports[i] = numGBSignalsGBReports[i]
					/ totalNumPlayers;
		}

		// write to rawData.m
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "rawData.m"));

		writerMatlab.write("MMsignalsMMreports = "
				+ Arrays.toString(numMMSignalsMMReports) + "';\n");
		writerMatlab.write("MMsignalsGBreports = "
				+ Arrays.toString(numMMSignalsGBReports) + "';\n");
		writerMatlab.write("GBsignalsMMreports = "
				+ Arrays.toString(numGBSignalsMMReports) + "';\n");
		writerMatlab.write("GBsignalsGBreports = "
				+ Arrays.toString(numGBSignalsGBReports) + "';\n");

		writerMatlab
				.write(String
						.format("fH = figure;\n"
								+ "hBar = bar(0:%d, [MMsignalsGBreports MMsignalsMMreports  GBsignalsGBreports GBsignalsMMreports], "
								+ "'BarWidth', 0.7, 'BarLayout', 'stack', 'LineStyle', 'none');\n"
								+ "box off;\n", expSet.numRounds - 1));
		if (treatment.equals("prior2-constant")) {
			writerMatlab.write("set(fH, 'Position', [300, 300, 800, 400]);\n"
					+ "set(gca,'Position',[.1 .15 .88 .8]);\n");
		} else {
			writerMatlab.write("set(fH, 'Position', [300, 300, 500, 400]);\n"
					+ "set(gca,'Position',[.15 .15 .8 .8]);\n");
		}
		writerMatlab
				.write("xlh = xlabel('Round');\n"
						+ "ylh = ylabel('Percentage of players');\n"
						+ "set(xlh, 'FontSize', 26);\n"
						+ "set(ylh, 'FontSize', 26);\n"
						+ "axes = findobj(gcf,'type','axes');\n"
						+ "set(axes, 'FontSize', 20);\n"
						+ "axis([-1 20 0 1]);\n"
						+ "axes = findobj(gcf,'type','axes');\n"
						+ "set(axes, 'XTick', [0 19]);\n"
						+ "set(hBar,{'FaceColor'},{[1 0.27 0];[1 0.64 0];'b';[0.1 0.1 0.4];});\n");
		if (treatment.equals("prior2-constant")) {
			writerMatlab
					.write("AX=legend('MM signals, GB reports', 'MM signals, MM reports', 'GB signals, GB reports', 'GB signals, MM reports', "
							+ "'Location', 'BestOutside');\n"
							+ "LEG = findobj(AX,'type','text');\n"
							+ "set(LEG,'FontSize',22);\n"
							+ "set(LEG, 'FontWeight', 'bold');\n");
		}
		writerMatlab.flush();
		writerMatlab.close();
	}

	public static void writeRawDataToFile() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "rawData.txt"));
		writer.write(String.format("number of players per game: %d\n"
				+ "number of rounds per game: %d\n"
				+ "total number of games: %d\n"
				+ "number of games without disconnected player: %d\n"
				+ "prior information: %s, %s\n", expSet.numPlayers,
				expSet.numRounds, expSet.numTotalGames, expSet.numGames,
				Arrays.toString(expSet.priorProbs), expSet.worlds));

		if (treatment.equals("prior2-basic")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and the report of "
					+ "another player randomly chosen among all other players, as follows:\n"
					+ "(MM, MM) = 1.5, (MM, GB) = 0.1, (GB, GB) = 1.2, (GB, MM) = 0.3\n"
					+ "where (A, B) = X denotes that if a player P's report is A, P's reference report is B, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-outputagreement")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and the report of "
					+ "another player randomly chosen among all other players, as follows:\n"
					+ "(MM, MM) = 1.5, (MM, GB) = 0.1, (GB, GB) = 1.5, (GB, MM) = 0.1\n"
					+ "where (A, B) = X denotes that if player P's report is A, P's reference report is B, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-uniquetruthful")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and all the other reports, as follows:\n"
					+ "(MM, 3) = 0.8, (MM, 2) = 1.5, (MM, 1) = 0.1, (MM, 0) = 0.9, \n"
					+ "(GB, 3) = 0.9, (GB, 2) = 0.1, (GB, 1) = 1.5, (GB, 0) = 0.8, \n"
					+ "where (A, B) = X denotes that if player P's report is A, B of the other 3 reports are MM, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writer.write("Payment rule:\n"
					+ "Each player's payoff depends on the player's report and all the other reports, as follows:\n"
					+ "(MM, 3) = 0.15, (MM, 2) = 1.50, (MM, 1) = 0.10, (MM, 0) = 0.10, \n"
					+ "(GB, 3) = 0.10, (GB, 2) = 0.15, (GB, 1) = 0.90, (GB, 0) = 0.15, \n"
					+ "where (A, B) = X denotes that if player P's report is A, B of the other 3 reports are MM, "
					+ "then P's payoff is X.\n\n");
		} else if (treatment.equals("prior2-constant")) {
			writer.write("Payment rule:\n"
					+ "Every player gets 0.90 for every round.\n\n");
		}

		int gameIndex = 0;
		for (Game game : expSet.games) {
			writer.write(String.format("Game %d\n", gameIndex));
			int playerIndex = 0;
			for (String hitId : game.playerHitIds) {
				writer.write(String.format("Player %d: ", playerIndex));
				for (int i = 0; i < game.rounds.size(); i++) {
					Round r = game.rounds.get(i);
					writer.write(String.format("(%s,%s)", r.getSignal(hitId),
							r.getReport(hitId)));

					if (i == game.rounds.size() - 1) {
						writer.write("\n");
					} else {
						writer.write(",");
					}
				}
				playerIndex++;
			}
			gameIndex++;
		}
		writer.flush();
		writer.close();
	}

	public static void writeAvgBonus() {
		System.out.println("Write average bonus");

		int numPlayersPerGame = expSet.numPlayers;
		int totalNumPlayers = numPlayersPerGame * expSet.numGames;

		double avgReward = 0;
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				for (Round round : game.rounds) {
					double reward = (double) round.result.get(hitId).get(
							"reward");
					avgReward += reward;
				}
			}
		}
		avgReward = avgReward / totalNumPlayers / expSet.numRounds;
		System.out.printf("Average reward: %.2f\n", avgReward);
	}

	

	// private static void strategyPayoffAnalysis(String rule) {
	//
	// System.out.println("\n" + "Strategy payoff analysis -- " + rule);
	//
	// double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule,
	// priorProbs, prior);
	// System.out.printf("Payoff at truthful equilibrium: %.6f\n",
	// truthfulPayoffT3);
	//
	// double mixedPayoff = 0.0;
	// double[] strategy = new double[2];
	//
	// double unit = 0.1;
	// int numUnit = Math.round((int)(1 / unit));
	//
	// for (int i = 0; i <= numUnit; i++) {
	// for (int j = 0; j <= numUnit; j++) {
	// double strategyMMGivenMM = unit * i;
	// double strategyMMGivenGB = unit * j;
	// double mixedPayoffT5 = AnalysisUtils.getMixedPayoff(rule,
	// priorProbs, prior, strategyMMGivenMM, strategyMMGivenGB);
	// // System.out.printf("mixed strategy is (%.2f, %.2f), payoff is %.6f\n",
	// // strategyMMGivenMM, strategyMMGivenGB, mixedPayoffT5);
	// if (mixedPayoffT5 > mixedPayoff) {
	// mixedPayoff = mixedPayoffT5;
	// strategy[0] = strategyMMGivenMM;
	// strategy[1] = strategyMMGivenGB;
	// }
	// }
	// }
	// System.out.printf("best mixed strategy; (%.2f, %.2f), best payoff; %.6f",
	// strategy[0], strategy[1], mixedPayoff);
	// }

	public static void learnHMM() throws IOException {

		System.out.println("\n\nLearning HMM");

		// Set number of strategies
		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {
			numStrategies = 4;
		} else if (treatment.equals("prior2-uniquetruthful")) {
			numStrategies = 4;
		} else if (treatment.equals("prior2-symmlowpay")) {
			numStrategies = 4;
		} else if (treatment.equals("prior2-constant")) {
			numStrategies = 4;
		}
		strategyNames = new String[numStrategies];
		System.out.printf("numStrategies: %d\n", numStrategies);

		// list of signal and report observations
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = LogReader.getActObsSequence();
		BWToleranceLearner bwl = new BWToleranceLearner();

		double loglk = Double.NEGATIVE_INFINITY;

		// load last best HMM if it exists
		File hmmFile = new File(String.format("%slearntHMM%dstrategies.txt",
				rootDir, numStrategies));
		if (hmmFile.exists()) {
			learntHmm = createHMMFromFile(numStrategies);
			loglk = BWToleranceLearner.computeLogLk(learntHmm, seq);
			System.out.printf("Starting loglikelihood : %.5f\n", loglk);
		}

		for (int i = 0; i < numRestarts; i++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = Utils.getRandomHmm(numStrategies);

			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = bwl
					.learn(origHmmTemp, seq);

			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp,
					seq);

			// Investigate robustness
			// if (loglkTemp > -2000) {
			// System.out.println("\nGood HMM:\n" + learntHmmTemp);
			// }

			if (loglkTemp > loglk) {

				origHmm = origHmmTemp;
				learntHmm = learntHmmTemp;
				loglk = loglkTemp;

				System.out.println(loglk);
				saveHMMDataToFile();
			}

		}

		// compute steady state prob
		double[] steadyState = calcSteadyStateProb(learntHmm);

		// write HMM to console
		System.out.printf("Ending loglikelihood : %.5f\n"
				+ "Resulting HMM: %s\n" + "Steady state probabilities: %s\n",
				loglk, learntHmm, Arrays.toString(steadyState));

		// write HMM to file
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ numStrategies + "StateHmm.txt"));
		writer.write(String.format("Ending loglikelihood : %.5f\n"
				+ "Resulting HMM: %s\n" + "Steady state probabilities: %s\n",
				loglk, learntHmm, Arrays.toString(steadyState)));
		writer.flush();
		writer.close();

	}

	public static void setStrategyNames() throws IOException {

		System.out.println("\nGive strategies names");

		for (int i = 0; i < numStrategies; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = learntHmm
					.getOpdf(i);

			
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1 = learntHmm
					.getOpdf(i);
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf2 = learntHmm
					.getOpdf(mmState);
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf3 = learntHmm
					.getOpdf(truthfulState);
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf4 = learntHmm
					.getOpdf(gbState);
			
			if (Utils.isMMStrategy(opdf)) {
				if (mmState == -1) {
					strategyNames[i] = "MM";
					mmState = i;
				} else if (Utils.isBetterMMStrategy(opdf1, opdf2)) {
					strategyNames[mmState] = "Mixed";
					mixedState = mmState;

					strategyNames[i] = "MM";
					mmState = i;
				}
			} else if (Utils.isGBStrategy(opdf)) {
				if (gbState == -1) {
					strategyNames[i] = "GB";
					gbState = i;
				} else if (Utils.isBetterGBStrategy(opdf1, opdf4)) {
					strategyNames[gbState] = "Mixed";
					mixedState = gbState;

					strategyNames[i] = "GB";
					gbState = i;
				}
			} else if (Utils.isTruthfulStrategy(opdf)) {
				if (truthfulState == -1) {
					strategyNames[i] = "Truthful";
					truthfulState = i;
				} else if (Utils.isBetterTruthfulStrategy(opdf1, opdf3)) {
					strategyNames[truthfulState] = "Mixed";
					mixedState = truthfulState;

					strategyNames[i] = "Truthful";
					truthfulState = i;
				}
			}

			if (strategyNames[i] == null) {
				if (mixedState == -1) {
					strategyNames[i] = "Mixed";
					mixedState = i;
				} else {
					strategyNames[i] = "Mixed2";
					mixed2State = i;
				}
			}
		}
		System.out.printf("Strategy names: %s\n"
				+ "MM strategy: %d, GB strategy: %d, "
				+ "Truthful strategy: %d, Mixed strategies: %d, %d\n",
				Arrays.toString(strategyNames), mmState, gbState,
				truthfulState, mixedState, mixed2State);
	}

	public static void writeStateSeq() throws IOException {

		System.out.println("Write state sequence");

		// Calculate most likely state sequence
		for (Game game : expSet.games) {
			game.stateSeq = new HashMap<String, int[]>();

			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> observList = game.signalReportObjList
						.get(hitId);
				ViterbiCalculator vi = new ViterbiCalculator(observList,
						learntHmm);
				int[] stateSeq = vi.stateSequence();
				game.stateSeq.put(hitId, stateSeq);
			}
		}

		// Write state sequence to csv
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "stateSeq" + numStrategies + "States.csv"));

		writer.write(String.format("hitId,"));
		for (int i = 1; i <= expSet.numRounds; i++) {
			writer.write(String.format("%d,", i));
		}
		writer.write(String.format("actual payoff\n"));

		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				writer.write(String.format("%s,", hitId));
				for (int state : stateSeq) {
					writer.write(state + ",");
				}
				writer.write(String.format("%.2f", game.actualPayoff.get(hitId)));
				writer.write("\n");
			}
			writer.write("\n");
		}
		writer.flush();
		writer.close();

	}

	public static void eqConvergenceHmm() throws IOException {

		System.out.println("Classify equilibrium convergence using HMM");

		for (Game game : expSet.games) {

			game.strategyComboTypeArray = new int[expSet.numRounds];

			for (int roundIndex = 0; roundIndex < expSet.numRounds; roundIndex++) {
				int[] numPlayerForStrategy = new int[numStrategies];
				for (String hitId : game.playerHitIds) {

					int strategyIndexForRound = game.stateSeq.get(hitId)[roundIndex];
					numPlayerForStrategy[strategyIndexForRound]++;
				}

				if (!treatment.equals("prior2-uniquetruthful")) {
					// other treatments

					for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
						if (numPlayerForStrategy[strategyIndex] == expSet.numPlayers
								&& (strategyIndex == mmState
										|| strategyIndex == gbState || strategyIndex == truthfulState)) {
							// all players are playing MM, GB or Truthful
							// strategy
							game.strategyComboTypeArray[roundIndex] = strategyIndex;
							break;
						} else if (numPlayerForStrategy[strategyIndex] > 0) {
							game.strategyComboTypeArray[roundIndex] = -1;
							break;
						}
					}
				} else {
					// "prior2-uniquetruthful"
					if (truthfulState != -1
							&& numPlayerForStrategy[truthfulState] == numStrategies) {
						// truthful equilibrium
						game.strategyComboTypeArray[roundIndex] = truthfulState;
					} else if (mixedState != -1
							&& numPlayerForStrategy[mixedState] == numStrategies) {
						// mixed strategy equilibrium
						game.strategyComboTypeArray[roundIndex] = mixedState;
					} else if (mmState != -1 && gbState != -1
							&& numPlayerForStrategy[mmState] == 1
							&& numPlayerForStrategy[gbState] == 3) {
						// 1 MM 3 GB
						game.strategyComboTypeArray[roundIndex] = 4;
					} else if (mmState != -1 && gbState != -1
							&& numPlayerForStrategy[mmState] == 3
							&& numPlayerForStrategy[gbState] == 1) {
						// 3 MM 1 GB
						game.strategyComboTypeArray[roundIndex] = 5;
					} else {
						// unclassified
						game.strategyComboTypeArray[roundIndex] = -1;
					}

				}
			}
		}

		// Write hmm type to CSV
		BufferedWriter writerCsv = new BufferedWriter(new FileWriter(rootDir
				+ "hmmType" + numStrategies + "Strategies.csv"));
		for (Game game : expSet.games) {
			writerCsv.write(String.format("%s,", game.id));
			for (int i = 0; i < game.strategyComboTypeArray.length; i++) {
				if (i == game.strategyComboTypeArray.length - 1) {
					writerCsv.write(String.format("%d\n",
							game.strategyComboTypeArray[i]));
				} else {
					writerCsv.write(String.format("%d,",
							game.strategyComboTypeArray[i]));
				}
			}
		}
		writerCsv.flush();
		writerCsv.close();

		// Determine hmm type count
		int[][] hmmTypeCount = new int[expSet.numRounds][numStrategies + 2];
		for (Game game : expSet.games) {
			for (int i = 0; i < expSet.numRounds; i++) {
				if (game.strategyComboTypeArray[i] == -1)
					continue;
				else
					hmmTypeCount[i][game.strategyComboTypeArray[i]]++;
			}
		}

		// Write hmmTypeCount to CSV file
		writerCsv = new BufferedWriter(new FileWriter(rootDir + "hmmTypeCount"
				+ numStrategies + "Strategies.csv"));

		// write headings
		for (int j = 0; j < numStrategies; j++) {
			writerCsv.write(String.format("%s,", strategyNames[j]));
		}
		if (treatment.equals("prior2-uniquetruthful")) {
			writerCsv.write(String.format("%s,", "1MM3GB"));
			writerCsv.write(String.format("%s,", "3MM1GB"));
		}
		writerCsv.write("\n");

		// write data
		for (int i = 0; i < expSet.numRounds; i++) {
			for (int j = 0; j < numStrategies; j++) {
				writerCsv.write(String.format("%d,", hmmTypeCount[i][j]));
			}
			if (treatment.equals("prior2-uniquetruthful")) {
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][numStrategies]));
				writerCsv.write(String.format("%d,",
						hmmTypeCount[i][numStrategies + 1]));
			}
			writerCsv.write("\n");
		}
		writerCsv.flush();
		writerCsv.close();

		// Write hmmTypeCount to matlab file
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "hmmType" + numStrategies + "Strategies.m"));

		// write data arrays
		for (int j = 0; j < numStrategies; j++) {

			if (treatment.equals("prior2-symmlowpay")) {
				// treatment 4
				if (strategyNames[j].equals("Mixed")
						|| strategyNames[j].equals("Mixed2")) {
					continue;
				}
			} else if (treatment.equals("prior2-uniquetruthful")) {
				// treatment 3, skip MM and GB strategies
				if (strategyNames[j].equals("MM")
						|| strategyNames[j].equals("GB")) {
					continue;
				}
			} else {
				// not treatment 3, skip mixed strategy
				if (strategyNames[j].equals("Mixed")) {
					continue;
				}
			}

			writerMatlab.write(String.format("%s = [", strategyNames[j]));
			for (int i = 0; i < expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][j] * 1.0 / expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");

		}
		if (treatment.equals("prior2-uniquetruthful")) {
			// write data for treatment 3
			writerMatlab.write(String.format("oneMMThreeGB = ["));
			for (int i = 0; i < expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][numStrategies] * 1.0
						/ expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");

			writerMatlab.write(String.format("threeMMOneGB = ["));
			for (int i = 0; i < expSet.numRounds; i++) {
				double percent = hmmTypeCount[i][numStrategies + 1] * 1.0
						/ expSet.numGames;
				writerMatlab.write(String.format("%.10f ", percent));
			}
			writerMatlab.write("]';\n");
		}

		writerMatlab.write(String.format("Unclassified = ["));
		for (int i = 0; i < expSet.numRounds; i++) {
			double num = 1.0;
			for (int j = 0; j < numStrategies; j++) {
				num -= hmmTypeCount[i][j] * 1.0 / expSet.numGames;
			}
			if (treatment.equals("prior2-uniquetruthful")) {
				num -= hmmTypeCount[i][numStrategies] * 1.0 / expSet.numGames;
				num -= hmmTypeCount[i][numStrategies + 1] * 1.0
						/ expSet.numGames;
			}
			num -= 0.0000000001;
			writerMatlab.write(String.format("%.10f ", num));
		}
		writerMatlab.write("]';\n\n");

		writerMatlab.write("fH = figure;\n"
				+ "set(fH, 'Position', [300, 300, 500, 400]);\n");

		// plot bar chart
		writerMatlab.write(String.format("hBar = bar(0:%d, [",
				expSet.numRounds - 1));
		if (treatment.equals("prior2-basic")) {
			writerMatlab.write("MM GB Truthful");
		} else if (treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("GB MM Truthful");
		} else if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("Mixed threeMMOneGB oneMMThreeGB ");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("MM");
		} else {
			writerMatlab.write("Truthful GB MM");
		}
		writerMatlab.write(String.format("], 'BarWidth', 0.7, "
				+ "'BarLayout', 'stack', 'LineStyle', 'none');\n"));

		// labels
		int xylabelfontSize = 26;
		int axisFontSize = 20;

		String ytick = "";
		if (treatment.equals("prior2-basic")) {
			ytick = "[0 0.45 0.54 1]";
		} else if (treatment.equals("prior2-outputagreement")) {
			ytick = "[0 0.36 0.46 1]";
		} else if (treatment.equals("prior2-uniquetruthful")) {
			ytick = "[0 0.12 0.17 1]";
		} else if (treatment.equals("prior2-symmlowpay")) {
			ytick = "[0.02 1]";
		} else {
			ytick = "[0 1]";
		}

		writerMatlab.write(String.format("box off;\n"
				+ "set(gca,'Position',[.03 .15 .8 .8]);\n"
				+ "xlh = xlabel('Round');\n" + "set(xlh, 'FontSize', %d);\n"
				+ "axes = findobj(gcf,'type','axes');\n"
				+ "set(axes, 'FontSize', %d);\n"
				+ "set(axes, 'XTick', [0 %d]);\n" + "B = axes;\n"
				+ "set(B, 'yaxislocation', 'right', 'ytick', %s);\n"
				+ "ylh = ylabel('Percentage of games');\n"
				+ "set(ylh, 'FontSize', %d);\n", xylabelfontSize, axisFontSize,
				expSet.numRounds - 1, ytick, xylabelfontSize));

		// set axis range
		writerMatlab.write(String.format("axis([-1 %d 0 1]);\n",
				expSet.numRounds));

		// write legend
		writerMatlab.write("lh = legend(");

		if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("'Mixed', 'threeMMOneGB', 'oneMMThreeGB', ");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("'MM', ");
		} else if (treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'GB', 'MM', 'Truthful', ");
		} else {
			writerMatlab.write("'MM', 'GB', 'Truthful', ");
		}
		writerMatlab.write(String.format("'Location', 'Best');\n"
				+ "set(lh, 'FontSize', 20);\n"));

		// set colors
		writerMatlab.write(String.format("set(hBar,{'FaceColor'},{"));
		if (treatment.equals("prior2-uniquetruthful")) {
			writerMatlab.write("[0.6 0.6 0.6];'y';'r';");
		} else if (treatment.equals("prior2-symmlowpay")) {
			writerMatlab.write("[1 0.64 0];");
		} else if (treatment.equals("prior2-outputagreement")) {
			writerMatlab.write("'b';[1 0.64 0];'g';");
		} else {
			writerMatlab.write("[1 0.64 0];'b';'g';");
		}
		writerMatlab.write(String.format("});\n"));

		writerMatlab.flush();
		writerMatlab.close();

	}

	public static void writeStrategyChangeHeatMap() throws IOException {

		System.out.println("Write hmm strategy change heat map");

		BufferedWriter writer1 = new BufferedWriter(new FileWriter(rootDir
				+ "heatMap" + numStrategies + "StrategiesReverseCompare.m"));

		List<int[]> seqList = new ArrayList<int[]>();
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] strategySeq = game.stateSeq.get(hitId);
				seqList.add(strategySeq);
			}
		}

		Collections.sort(seqList, new Comparator<int[]>() {

			@Override
			public int compare(int[] o1, int[] o2) {
				return util(o1, o2, o1.length - 1);
			}

			private int util(int[] o1, int[] o2, int index) {
				if (index == 0) {
					return o1[index] - o2[index];
				} else {
					if (o1[index] < o2[index])
						return -1;
					else if (o1[index] > o2[index])
						return 1;
					else
						return util(o1, o2, index - 1);
				}

			}

		});

		List<int[]> newSeqList = new ArrayList<int[]>();

		for (int endStrIndex = 0; endStrIndex < 4; endStrIndex++) {

			boolean blockFound = false;
			boolean inBlock = false;

			List<int[]> str0List = new ArrayList<int[]>();
			List<int[]> str1List = new ArrayList<int[]>();
			List<int[]> str2List = new ArrayList<int[]>();
			List<int[]> str3List = new ArrayList<int[]>();

			for (int seqIndex = 0; seqIndex < seqList.size(); seqIndex++) {

				int[] seq = seqList.get(seqIndex);

				if (!inBlock) {
					if (seq[expSet.numRounds - 1] == endStrIndex) {
						blockFound = true;
						inBlock = true;
					}
				} else {
					if (seq[expSet.numRounds - 1] != endStrIndex)
						inBlock = false;
				}

				if (blockFound == false) {
					continue;
				} else if (inBlock == false) {
					break;
				} else {
					if (seq[0] == 0)
						str0List.add(seq);
					else if (seq[0] == 1)
						str1List.add(seq);
					else if (seq[0] == 2)
						str2List.add(seq);
					else if (seq[0] == 3)
						str3List.add(seq);
				}
			}

			newSeqList.addAll(str0List);
			newSeqList.addAll(str1List);
			newSeqList.addAll(str2List);
			newSeqList.addAll(str3List);

		}

		seqList.clear();
		seqList.addAll(newSeqList);

		int index = 0;
		for (int i = 0; i < seqList.size(); i++) {
			int[] strategySeq = seqList.get(i);
			writer1.write(String.format("x%d = %s;\n", index,
					Arrays.toString(strategySeq)));
			index++;
		}

		index = 0;
		writer1.write("m = [");
		for (int i = 0; i < seqList.size(); i++) {
			writer1.write(String.format("x%d;", index));
			index++;
		}
		writer1.write("];\n\n");
		writer1.write("api_path = '~/plotly';\n"
				+ "addpath(genpath(api_path));\n" + "api_key = 'vivdmp4vf4';\n"
				+ "username = 'alice.gao';\n" + "signin(username, api_key);\n");

		String truthfulColor = "50,205,50";
		String gbColor = "0,0,255";
		String mixedColor = "190,190,190";
		String mixed2Color = "127,127,127";
		String mmColor = "255,140,0";

		writer1.write("cs={{");
		for (int i = 0; i <= 3; i++) {
			double num = i * 1.0 / 3;
			if (mmState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, mmColor));
			} else if (gbState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num, gbColor));
			} else if (truthfulState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						truthfulColor));
			} else if (mixedState == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						mixedColor));
			} else if (mixed2State == i) {
				writer1.write(String.format("{%.2f, 'rgb(%s)'},", num,
						mixed2Color));
			}
		}
		writer1.write("}};\n");
		writer1.write("plotly({struct('z', m,'scl',cs,'type', 'heatmap')})\n");
		writer1.flush();
		writer1.close();
	}

	public static void writeStrategyDistribution() throws IOException {

		System.out.println("Write hmm strategy distribution");

		// number of players playing each strategy in each round

		int[][] strategyCount = new int[numStrategies][expSet.numRounds];
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				int[] strategySeq = game.stateSeq.get(hitId);
				for (int roundIndex = 0; roundIndex < expSet.numRounds; roundIndex++) {
					int strategyIndex = strategySeq[roundIndex];
					strategyCount[strategyIndex][roundIndex]++;
				}
			}
		}
		double[][] strategyDistribution = new double[numStrategies][expSet.numRounds];
		int totalNumPlayers = expSet.numGames * expSet.numPlayers;
		for (int roundIndex = 0; roundIndex < expSet.numRounds; roundIndex++) {
			for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
				strategyDistribution[strategyIndex][roundIndex] = strategyCount[strategyIndex][roundIndex]
						* 1.0 / totalNumPlayers;
			}
		}

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "strategyDistribution" + numStrategies + "Strategies.m"));

		for (int strategyIndex = 0; strategyIndex < numStrategies; strategyIndex++) {
			writer.write(strategyNames[strategyIndex] + " = [");
			for (int roundIndex = 0; roundIndex < expSet.numRounds; roundIndex++) {
				writer.write(String.format("%.10f ",
						strategyDistribution[strategyIndex][roundIndex]));
			}
			writer.write("]';\n");
		}

		writer.write(String.format("\n\n" + "figure;\n" + "hBar = bar(1:%d, ",
				expSet.numRounds));
		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer.write("[Truthful MM Mixed Mixed2]");
			else if (numStrategies == 3)
				writer.write("[Truthful MM Mixed]");
		} else {
			writer.write("[Truthful GB MM Mixed]");
		}
		writer.write(", 0.5, 'stack');\n");

		writer.write("xlh = xlabel('Round');\n"
				+ "ylh = ylabel('Percentage of players');\n"
				+ "set(xlh, 'FontSize', 26);\n" + "set(ylh, 'FontSize', 26);\n"
				+ "axes = findobj(gcf,'type','axes');\n"
				+ "set(axes, 'FontSize', 20);\n");

		writer.write(String.format("axis([0 %d 0 1]);\n", expSet.numRounds + 1));

		// legend
		writer.write("lh = legend(");
		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer.write("'Truthful', 'MM', 'Mixed', 'Mixed2'");
			else if (numStrategies == 3)
				writer.write("'Truthful', 'MM', 'Mixed'");
		} else {
			writer.write("'Truthful', 'GB', 'MM', 'Mixed'");
		}
		writer.write(", 'Location', 'Best');\n" + "set(lh, 'FontSize', 20);\n");

		if (treatment.equals("prior2-symmlowpay")) {
			if (numStrategies == 4)
				writer.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.5 0.5 0.5];[0.8 0.8 0.8];});\n");
			else if (numStrategies == 3)
				writer.write("set(hBar,{'FaceColor'},{'g';[1 0.64 0];[0.8 0.8 0.8];});\n");
		} else {
			writer.write("set(hBar,{'FaceColor'},{'g';'b';[1 0.64 0];[0.8 0.8 0.8];});\n");
		}
		writer.flush();
		writer.close();
	}

	public static void genPredictedStrategyChangeCode() throws IOException {

		System.out.println("Write hmm predicted strategy change");

		// Predicted strategy change
		BufferedWriter writer3 = new BufferedWriter(new FileWriter(rootDir
				+ "predictedStrategyChange.m"));

		writer3.write("a = [");
		for (int i = 0; i < numStrategies; i++) {
			for (int j = 0; j < numStrategies; j++) {
				double aij = learntHmm.getAij(i, j);
				writer3.write(" " + aij);
				if (j < numStrategies - 1)
					writer3.write(",");
				else
					writer3.write(";");
			}
		}
		writer3.write("];\n");

		writer3.write("p = [");
		for (int i = 0; i < numStrategies; i++) {
			writer3.write(learntHmm.getPi(i) + ",");
		}
		writer3.write("];\n");

		writer3.write("m = zeros(50," + numStrategies + ");\n"
				+ "m(1,:) = p;\n" + "for i =2:50\n" + "m(i,:) = m(i-1,:)*a;\n"
				+ "end\n" + "x = 1:50;\n" + "plot(");
		for (int i = 1; i <= numStrategies; i++) {
			writer3.write("x, m(:," + i + ")");
			if (i < numStrategies)
				writer3.write(",");
		}
		writer3.write(")\n");

		writer3.write("legend(");
		for (int i = 0; i < numStrategies; i++) {
			writer3.write("'" + strategyNames[i] + "'");
			if (i < numStrategies - 1)
				writer3.write(",");
		}
		writer3.write(")");

		writer3.flush();
		writer3.close();
	}

	public static void graphLogLikelihood() throws IOException {

		System.out.println("Graph log likelihood");

		List<List<SigActObservation<CandySignal, CandyReport>>> seq = LogReader.getActObsSequence();
		double loglk;

		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ "logLikelihood.m"));

		if (treatment.equals("prior2-basic"))
			writerMatlab.write("treatment1loglk = [");
		else if (treatment.equals("prior2-outputagreement"))
			writerMatlab.write("treatment2loglk = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writerMatlab.write("treatment3loglk = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writerMatlab.write("treatment4loglk = [");
		else if (treatment.equals("prior2-constant"))
			writerMatlab.write("treatment5loglk = [");

		for (int numStates = 2; numStates <= 6; numStates++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(numStates);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);

			writerMatlab.write(String.format("%.6f ", loglk));
		}
		writerMatlab.write("];\n");

		System.out.println("Graph Bayesian information criterion");
		double bic;

		if (treatment.equals("prior2-basic"))
			writerMatlab.write("treatment1bic = [");
		else if (treatment.equals("prior2-outputagreement"))
			writerMatlab.write("treatment2bic = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writerMatlab.write("treatment3bic = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writerMatlab.write("treatment4bic = [");
		else if (treatment.equals("prior2-constant"))
			writerMatlab.write("treatment5bic = [");

		for (int numStates = 2; numStates <= 6; numStates++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(numStates);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);

			int numParams = (numStates * numStates + 2 * numStates - 1);
			int numData = expSet.numGames * expSet.numPlayers
					* expSet.numRounds;
			bic = -2 * loglk + numParams * Math.log(numData);

			writerMatlab.write(String.format("%.6f ", bic));
		}
		writerMatlab.write("];\n");

		// writerMatlab.write("x = 2:6;\n");
		// writerMatlab.write("figure;\n");
		// writerMatlab.write("plot(x,loglk)\n");
		// writerMatlab.write("xlabel('Number of strategies');\n");
		// writerMatlab.write("ylabel('Log likelihood');\n");
		// writerMatlab.write("set(gca,'XTick',2:6);\n");
		writerMatlab.flush();
		writerMatlab.close();

	}

	private static double[] calcSteadyStateProb(
			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm) {
		int numStates = learntHmm.nbStates();
		RealMatrix Aij = new Array2DRowRealMatrix(numStates, numStates);
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				double val = learntHmm.getAij(i, j);
				Aij.setEntry(i, j, val);
			}
		}

		int i = 15;
		while (i > 0) {
			Aij = Aij.multiply(Aij);
			i--;
		}
		return Aij.getRow(0);
	}

	private static void saveHMMDataToFile() throws IOException {
		BufferedWriter writer = new BufferedWriter(new FileWriter(
				String.format("%slearntHMM%dstrategies.txt", rootDir,
						numStrategies)));
		int numStates = learntHmm.nbStates();
		writer.write(String.format("numStates:%d\n", numStates));

		for (int i = 0; i < numStates; i++) {
			writer.write(String.format("pi,%d:%.17f\n", i, learntHmm.getPi(i)));
		}

		for (int i = 0; i < numStates; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = learntHmm
					.getOpdf(i);
			double reportMMGivenSignalMM = opdf
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double reportMMGivenSignalGB = opdf
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			writer.write(String.format("opdf,%d:%.17f,%.17f\n", i,
					reportMMGivenSignalMM, reportMMGivenSignalGB));
		}

		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				writer.write(String.format("aij,%d,%d:%.17f\n", i, j,
						learntHmm.getAij(i, j)));
			}
		}
		writer.flush();
		writer.close();
	}

	private static Hmm<SigActObservation<CandySignal, CandyReport>> createHMMFromFile(
			int numStr) throws IOException {
		BufferedReader reader = new BufferedReader(new FileReader(
				String.format("%slearntHMM%dstrategies.txt", rootDir, numStr)));
		String line = reader.readLine();
		Matcher matcher = Pattern.compile("numStates:(.*)").matcher(line);
		int numStates = -1;
		if (matcher.matches())
			numStates = Integer.parseInt(matcher.group(1));
		// System.out.printf("num states: %d\n", numStates);

		double[] pi = new double[numStates];
		for (int i = 0; i < numStates; i++) {
			if (i == numStates - 1) {
				line = reader.readLine();
				double left = 1.0;
				for (int j = 0; j < numStates - 1; j++) {
					left -= pi[j];
				}
				pi[numStates - 1] = left;
			} else {
				line = reader.readLine();
				matcher = Pattern.compile("pi,(.*):(.*)").matcher(line);
				if (matcher.matches()) {
					pi[i] = Double.parseDouble(matcher.group(2));
				}
			}
		}
		// System.out.printf("pi: %s\n", Arrays.toString(pi));

		List<OpdfStrategy<CandySignal, CandyReport>> opdfs = new ArrayList<OpdfStrategy<CandySignal, CandyReport>>();

		for (int i = 0; i < numStates; i++) {
			line = reader.readLine();
			matcher = Pattern.compile("opdf,(.*):(.*),(.*)").matcher(line);
			if (matcher.matches()) {
				double[][] probs = new double[2][2];
				probs[0][0] = Double.parseDouble(matcher.group(2));
				probs[0][1] = 1 - probs[0][0];
				probs[1][0] = Double.parseDouble(matcher.group(3));
				probs[1][1] = 1 - probs[1][0];
				OpdfStrategy<CandySignal, CandyReport> opdf = new OpdfStrategy<CandySignal, CandyReport>(
						CandySignal.class, CandyReport.class, Utils.signalPrior,
						probs);
				// System.out.printf("opdf,%d:%s", i, opdf.toString());
				opdfs.add(opdf);
			}
		}

		double[][] aij = new double[numStates][numStates];
		for (int i = 0; i < numStates; i++) {
			for (int j = 0; j < numStates; j++) {
				line = reader.readLine();
				if (j == numStates - 1) {
					double left = 1.0;
					for (int jj = 0; jj < numStates - 1; jj++) {
						left -= aij[i][jj];
					}
					aij[i][numStates - 1] = left;
				} else {
					matcher = Pattern.compile("aij,(.*),(.*):(.*)").matcher(
							line);
					if (matcher.matches()) {
						int ii = Integer.parseInt(matcher.group(1));
						int jj = Integer.parseInt(matcher.group(2));
						double prob = Double.parseDouble(matcher.group(3));
						aij[ii][jj] = prob;
					}
				}
			}
		}
		// System.out.println("Aij:");
		// for (int i = 0; i < numStates; i++) {
		// System.out.println(Arrays.toString(aij[i]));
		// }
		reader.close();
		Hmm<SigActObservation<CandySignal, CandyReport>> hmm = new Hmm<SigActObservation<CandySignal, CandyReport>>(
				pi, aij, opdfs);
		return hmm;
	}

	public static void eqConvergenceSimpleMethod() throws IOException {
	
		System.out
				.println("Equilibrium Convergence Analysis Using Simple Method");
	
		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")
				|| treatment.equals("prior2-symmlowpay")) {
			gameSymmetricConvergenceType();
			// gameSymmetricConvergenceTypeRelaxed(3);
		} else if (treatment.equals("prior2-uniquetruthful")) {
			gameConvergenceTypeT3();
			// gameAsymmetricConvergenceTypeRelaxed(3);
		}
	}

	private static void gameSymmetricConvergenceType() throws IOException {

		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;
		int numTotal = expSet.numGames;

		for (Game game : expSet.games) {

			game.fillConvergenceType();

			if (game.convergenceType.equals("MM"))
				numMM++;
			else if (game.convergenceType.equals("GB"))
				numGB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else
				numUnclassified++;
		}

		System.out
				.println(String
						.format("GB: %d (%d%%), MM: %d (%d%%), HO: %d (%d%%), Unclassified: %d(%d%%), Total: %d",
								numGB, Math.round(numGB * 100.0 / numTotal),
								numMM, Math.round(numMM * 100.0 / numTotal),
								numHO, Math.round(numHO * 100.0 / numTotal),
								numUnclassified,
								Math.round(numUnclassified * 100.0 / numTotal),
								numTotal));
	}

	private static void gameConvergenceTypeT3() {

		int numHO = 0;
		int num3MM1GB = 0;
		int num1MM3GB = 0;
		int numUnclassified = 0;

		for (Game game : expSet.games) {

			game.fillAsymmetricConvergenceType();

			if (game.convergenceType.equals("3MM"))
				num3MM1GB++;
			else if (game.convergenceType.equals("3GB"))
				num1MM3GB++;
			else if (game.convergenceType.equals("HO"))
				numHO++;
			else
				numUnclassified++;
		}

		System.out.println(String.format(
				"3GB: %d, 3MM: %d, HO: %d, Unclassified: %d, Total: %d",
				num1MM3GB, num3MM1GB, numHO, numUnclassified, expSet.numGames));

	}

	private static void gameSymmetricConvergenceTypeRelaxed(int i)
			throws IOException {

		int numHO = 0;
		int numMM = 0;
		int numGB = 0;
		int numUnclassified = 0;

		for (Game game : expSet.games) {
			game.fillConvergenceTypeRelaxed(i);

			if (game.convergenceTypeRelaxed.startsWith("MM"))
				numMM++;
			else if (game.convergenceTypeRelaxed.startsWith("GB"))
				numGB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else
				numUnclassified++;
		}

		System.out.println(String.format(
				"i=%d, GB: %d, MM: %d, HO: %d, Unclassified: %d", i, numGB,
				numMM, numHO, numUnclassified));

	}

	private static void gameAsymmetricConvergenceTypeRelaxed(int i)
			throws IOException {

		int numHO = 0;
		int num3MM = 0;
		int num3GB = 0;
		int numUnclassified = 0;

		for (Game game : expSet.games) {

			game.fillAsymmetricConvergenceTypeRelaxed(i);

			if (game.convergenceTypeRelaxed.startsWith("3MM"))
				num3MM++;
			else if (game.convergenceTypeRelaxed.startsWith("3GB"))
				num3GB++;
			else if (game.convergenceTypeRelaxed.startsWith("HO"))
				numHO++;
			else
				numUnclassified++;
		}

		System.out.println(String.format(
				"i=%d, 3GB: %d, 3MM: %d, HO: %d, Unclassified: %d", i, num3GB,
				num3MM, numHO, numUnclassified));
	}

	private static void parseLog() {

		// System.out.println("Parsing game log");

		if (treatment.equals("prior2-constant")
				|| treatment.equals("prior2-symmlowpay"))
			MatchStrings.choseReport = MatchStrings.chosenReport1;
		else
			MatchStrings.choseReport = MatchStrings.chosenReport2;

		expSet = new Experiment();
		expSet.setId = setId;

		Connection con = null;

		// get experiment log
		Statement expStmt = null;
		ResultSet expRS = null;

		// get bonus
		Statement bonusStmt = null;
		ResultSet bonusRS = null;

		// get round log
		Statement roundStmt = null;
		ResultSet roundRS = null;

		// get exit survey
		Statement exitSurveyStmt = null;
		ResultSet exitSurveyRS = null;

		try {
			Class.forName(dbClass);
			con = DriverManager.getConnection(dbUrl, "root", "");

			// get total number of games
			String numGameQuery = String
					.format("select count(*) from experiment where setId='%s' and inputdata='%s'",
							setId, treatment);
			Statement numGameStmt = con.createStatement();
			ResultSet numGameRS = numGameStmt.executeQuery(numGameQuery);
			numGameRS.next();
			expSet.numTotalGames = numGameRS.getInt(1);

			String expQuery = String
					.format("select * from experiment "
							+ "where setId='%s' and inputdata = '%s' "
							+ "and id not in (select distinct experimentId from round where results like '%%killed%%')",
							setId, treatment);
			expStmt = con.createStatement();
			expRS = expStmt.executeQuery(expQuery);

			while (expRS.next()) {
				String gameId = expRS.getString("id");
				String expLog = expRS.getString("results");

				Game game = parseGameLog(expLog);
				game.id = gameId;

				for (int i = 0; i < game.numRounds; i++) {

					String roundQuery = String.format("select * from round "
							+ "where experimentId = '%s' and roundnum = %d",
							gameId, i + 1);
					roundStmt = con.createStatement();
					roundRS = roundStmt.executeQuery(roundQuery);

					String roundLog = "";
					if (roundRS.next()) {
						roundLog = roundRS.getString("results");
					}
					Round round = parseRoundLog(roundLog, game);
					game.addRound(round);
				}

				game.saveSignalReportPairList();

				// Save exit survey objects
				for (String hitId : game.playerHitIds) {
					String exitSurveyQuery = String.format(
							"select comment from session "
									+ "where comment is not null "
									+ "and experimentId='%s' "
									+ "and hitId='%s'", gameId, hitId);
					exitSurveyStmt = con.createStatement();
					exitSurveyRS = exitSurveyStmt.executeQuery(exitSurveyQuery);
					exitSurveyRS.next();

					String comment = exitSurveyRS.getString("comment");
					ExitSurvey exitSurvey = new ExitSurvey(comment);
					// exitSurveys.put(hitId, comment);
					game.exitSurvey.put(hitId, exitSurvey);
				}

				String bonusQuery = String
						.format("select hitId, bonus from session where experimentId='%s'",
								gameId);
				bonusStmt = con.createStatement();
				bonusRS = bonusStmt.executeQuery(bonusQuery);
				while (bonusRS.next()) {
					String hitId = bonusRS.getString("hitId");
					double bonus = Double.parseDouble(bonusRS
							.getString("bonus"));
					game.actualPayoff.put(hitId, bonus);
				}

				expSet.addGame(game);
			}

			// set parameters for this treatment
			expSet.numGames = expSet.games.size();
			expSet.numPlayers = expSet.games.get(0).numPlayers;
			expSet.numRounds = expSet.games.get(0).numRounds;
			expSet.worlds = expSet.games.get(0).worlds;
			expSet.priorProbs = expSet.games.get(0).priorProbs;

		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		} catch (SQLException e) {
			e.printStackTrace();
		} finally {
			try {
				if (expRS != null)
					expRS.close();
				if (roundRS != null)
					roundRS.close();
				if (expStmt != null)
					expStmt.close();
				if (con != null)
					con.close();
			} catch (SQLException e) {
				e.printStackTrace();
			}
		}
	}

	private static Game parseGameLog(String experimentLogString) {
		Game gameObj = new Game();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;

		try {
			sc = new Scanner(experimentLogString);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = MatchStrings.experimentStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment start message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment start message", lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.priorPattern.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Prior message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
				gameObj.savePriorProb(matcher.group(2));
				gameObj.savePriorWorlds(matcher.group(3));
			} else
				throw new ParseException("Did not find prior message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.generalInfo.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("General information message found");
				// System.out.printf("%s, %s, %s, %s, %s, %s\n\n",
				// matcher.group(1), matcher.group(2), matcher.group(3),
				// matcher.group(4), matcher.group(5), matcher.group(6));
				gameObj.numPlayers = Integer.parseInt(matcher.group(2));
				gameObj.numRounds = Integer.parseInt(matcher.group(3));
				gameObj.savePlayerHitIds(matcher.group(4));
				gameObj.savePaymentRule(matcher.group(5));
				// gameInfo.saveSignalList(matcher.group(6));
			} else
				throw new ParseException(
						"Did not find general information message", lineIndex);

			int numRounds = gameObj.numRounds;

			for (int i = 0; i < numRounds; i++) {
				currentLine = sc.nextLine();
				lineIndex++;
				matcher = MatchStrings.experimentRoundStart
						.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round start message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round start message",
							lineIndex);

				currentLine = sc.nextLine();
				lineIndex++;
				matcher = MatchStrings.experimentRoundFinish
						.matcher(currentLine);
				if (matcher.matches()) {
					// System.out.println("Experiment round finish message found");
					// System.out.printf("%s, %s\n\n", matcher.group(1),
					// matcher.group(2));
				} else
					throw new ParseException(
							"Did not find experiment round finish message",
							lineIndex);
			}

			currentLine = sc.nextLine();
			lineIndex++;
			matcher = MatchStrings.experimentFinish.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Experiment finish message found");
				// System.out.printf("%s, %s, %s\n\n", matcher.group(1),
				// matcher.group(2), matcher.group(3));
			} else
				throw new ParseException(
						"Did not find experiment finish message", lineIndex);

		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}

		return gameObj;
	}

	private static Round parseRoundLog(String roundResults, Game game) {
		Round roundInfo = new Round();

		Scanner sc = null;
		String currentLine = null;
		int lineIndex = 0;
		try {
			sc = new Scanner(roundResults);

			// parse round start
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcher = MatchStrings.roundStart.matcher(currentLine);
			if (matcher.matches()) {
				// System.out.println("Round start message found");
				// System.out.printf("%s, %s\n\n", matcher.group(1),
				// matcher.group(2));
				roundInfo.roundNum = Integer.parseInt(matcher.group(2));
			} else
				throw new ParseException("Did not find round start message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherChosenWorld = MatchStrings.chosenWorld
					.matcher(currentLine);
			if (matcherChosenWorld.matches()) {
				// System.out.println("Chosen world message found");
				// System.out.printf("%s, %s\n\n", matcherChosenWorld.group(1),
				// matcherChosenWorld.group(2));
				roundInfo.saveChosenWorld(matcherChosenWorld.group(2));
			} else
				throw new ParseException("Did not find chosen world message",
						lineIndex);

			int numPlayers = game.numPlayers;
			int count = 0;
			while (count < numPlayers) {
				currentLine = sc.nextLine();
				lineIndex++;

				Matcher matcherSignal = MatchStrings.gotSignal
						.matcher(currentLine);
				if (matcherSignal.matches()) {
					// System.out.println("Signal message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherSignal.group(1), matcherSignal.group(2),
					// matcherSignal.group(3), matcherSignal.group(4));
					// roundInfo.saveSignal(matcherSignal.group(2),
					// matcherSignal.group(3), matcherSignal.group(4),
					// matcherSignal.group(1));
				} else {
					throw new ParseException(
							"Expected a signal message but did not find one",
							lineIndex);
				}
				count++;
			}

			// parse get signal and choose report messages
			int limit = numPlayers;
			count = 0;
			// boolean killedFound = false;
			while (count < limit) {
				currentLine = sc.nextLine();
				lineIndex++;

				// Matcher matcherKilledMsg = killed .matcher(currentLine);
				Matcher matcherReport = MatchStrings.choseReport
						.matcher(currentLine);
				// Matcher matcherFakeReport = fakeReport.matcher(currentLine);

				// if (matcherKilledMsg.matches()) {
				// // killed message found, there are 3 messages in this section
				// if (killedFound == false) {
				// killedFound = true;
				// limit++;
				// }
				// // write into game, round num, worker id, disconnected time
				//
				// } else
				if (matcherReport.matches()) {

					// System.out.println("Real report message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherReport.group(1), matcherReport.group(2),
					// matcherReport.group(3), matcherReport.group(4));
					// roundInfo.saveReport(matcherReport.group(2),
					// matcherReport.group(3), matcherReport.group(4),
					// matcherReport.group(1));

				}
				// else if (matcherFakeReport.matches()) {
				// // fake report found, there are 3 messages in this section
				//
				// // System.out.println("Fake report message found");
				// // System.out.printf("%s, %s, %s, %s\n\n",
				// // matcherFakeReport.group(1),
				// // matcherFakeReport.group(2),
				// // matcherFakeReport.group(3),
				// // matcherFakeReport.group(4));
				//
				// roundInfo.saveReport(matcherFakeReport.group(2),
				// matcherFakeReport.group(3), matcherFakeReport.group(4),
				// matcherFakeReport.group(1));
				// }
				else {
					throw new ParseException(
							"Did not find real or fake report messages.",
							lineIndex);
				}
				count++;
			}

			// parse round result
			currentLine = sc.nextLine();
			lineIndex++;
			Matcher matcherResult = MatchStrings.roundResult
					.matcher(currentLine);
			if (matcherResult.matches()) {
				// System.out.println("Round result message found");
				// System.out.printf("%s, %s\n\n", matcherResult.group(1),
				// matcherResult.group(2));
				roundInfo.saveResult(matcherResult.group(2));
			} else
				throw new ParseException("Did not find round result message",
						lineIndex);

			currentLine = sc.nextLine();
			lineIndex++;

			while (true) {
				Matcher matcherBonus = MatchStrings.getBonus
						.matcher(currentLine);
				Matcher matcherNoBonus = MatchStrings.noBonus
						.matcher(currentLine);
				if (matcherBonus.matches() || matcherNoBonus.matches()) {
					// System.out.println("Get bonus message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherBonus.group(1), matcherBonus.group(2),
					// matcherBonus.group(3), matcherBonus.group(4));

					currentLine = sc.nextLine();
					lineIndex++;
				} else {
					break;
				}
			}

			// parse round end
			Matcher matcherEnd = MatchStrings.roundEnd.matcher(currentLine);
			if (matcherEnd.matches()) {
				// System.out.println("Round end message found");
				// System.out.printf("%s, %s\n\n", matcherEnd.group(1),
				// matcherEnd.group(2));
				String endTimeString = matcherEnd.group(1);
				roundInfo.endTime = endTimeString;
				int min = Integer.parseInt(endTimeString.substring(0, 2));
				int sec = Integer.parseInt(endTimeString.substring(3, 5));
				int millisec = Integer.parseInt(endTimeString.substring(6, 9));
				int totalMillisec = min * 60 * 1000 + sec * 1000 + millisec;
				roundInfo.duration = totalMillisec;
			} else
				throw new ParseException("Did not find round end message",
						lineIndex);

			return roundInfo;

		} catch (ParseException e) {
			e.printStackTrace();
		} catch (FileNotFoundException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		} finally {
			if (sc != null)
				sc.close();
		}
		return null;
	}

	private static void writePlayerComments() throws IOException {

		// System.out.println("Parsing player comments");

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "playerComments.csv"));
		writer.write("gameId,hitId,actions,bonus,strategy,otherStrategy,reason,change,comments\n");

		for (Game game : expSet.games) {

			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> signalReportPairs = game.signalReportObjList
						.get(hitId);
				ExitSurvey survey = game.exitSurvey.get(hitId);

				if (survey == null)
					writer.write(String.format(
							"%s,%s,\"\"\"%s\"\"\",null,null,null,null,null\n",
							game.id, hitId, signalReportPairs));
				else
					writer.write(String.format("%s,%s," + "\"\"\"%s\"\"\","
							+ "%.2f," + "\"\"\"%s\"\"\"," + "\"\"\"%s\"\"\","
							+ "\"\"\"%s\"\"\"," + "\"\"\"%s\"\"\","
							+ "\"\"\"%s\"\"\"\n", game.id, hitId,
							signalReportPairs, game.actualPayoff.get(hitId),
							survey.checkedStrategies, survey.otherStrategy,
							survey.strategyReason, survey.strategyChange,
							survey.comments));
			}
			writer.write("\n");
		}

		writer.flush();
		writer.close();

	}

	public static void printInfo() {
		System.out.printf("treatment: %s\n" + "total num of games: %d\n"
				+ "non-killed games: %d\n" + "Prior probs: %s\n"
				+ "Prior worlds: %s\n" + "numPlayers / game: %d\n"
				+ "numRounds: %d\n\n", treatment, expSet.numTotalGames,
				expSet.numGames, Arrays.toString(expSet.priorProbs),
				expSet.worlds, expSet.numPlayers, expSet.numRounds);

	}

	private static void learningAnalysis(String type) throws IOException {
	
		if (treatment.equals("prior2-constant")) {
			System.out
					.println("Skipping learning analysis for constant payment treatment");
			return;
		}
	
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ type + ".csv"));
		writer.write("hitId, actual payoff, simulated payoff, improvement\n");
	
		double diffTotal = 0;
		int count = 0;
		for (Game game : expSet.games) {
	
			for (String hitId : game.playerHitIds) {
	
				double[] myPayoffs = new double[expSet.numRounds];
				String[] myReports = new String[expSet.numRounds];
	
				for (int i = 0; i < expSet.numRounds; i++) {
	
					String myReport = null;
					if (i == 0) {
	
						myReport = game.rounds.get(i).getReport(hitId);
	
					} else {
	
						Map<String, Double> oppPopStrategy = null;
						
						if (type.equals("BR")) {
							oppPopStrategy = game.getOppPopStrPrevRound(i,
									hitId);
						} else if (type.equals("FP")) {
							oppPopStrategy = game.getOppPopStrFull(i, hitId);
						}
	
						if (treatment.equals("prior2-uniquetruthful")
								|| treatment.equals("prior2-symmlowpay")) {
							myReport = Utils.getBestResponse(treatment, oppPopStrategy);
						} else {
							myReport = Utils.getBestResponse(treatment, oppPopStrategy);
						}
					}
	
					myReports[i] = myReport;
	
					if (treatment.equals("prior2-uniquetruthful")
							|| treatment.equals("prior2-symmlowpay")) {
						List<String> refReports = game.getRefReports(hitId, i);
						int numMM = Utils.getNumMMInRefReports(refReports);
						myPayoffs[i] = Utils
								.getPaymentTreatmentUniqueTruthful(myReport,
										numMM);
					} else {
						String refReport = game.getRefReport(i, hitId);
						myPayoffs[i] = Utils.getPayment(treatment, myReport, refReport);
					}
				}
	
				double totalPayoff = 0;
				for (int i = 0; i < expSet.numRounds; i++) {
					totalPayoff += myPayoffs[i];
				}
				double simulatedAvgPayoff = totalPayoff / expSet.numRounds;
				game.simulatedFPPayoff.put(hitId, simulatedAvgPayoff);
	
				// System.out.print(String.format("%s: %s, %.2f\n\n",
				// hitId, Arrays.toString(myReports), simulatedAvgPayoff));
	
				double actualAvgPayoff = game.actualPayoff.get(hitId);
	
				double diff = simulatedAvgPayoff - actualAvgPayoff;
				diffTotal += diff;
				count++;
	
				// Print out information
				writer.write(String.format("%s, %.2f, %.2f, %.2f\n", hitId,
						actualAvgPayoff, simulatedAvgPayoff, diff));
				// System.out.print(String.format("%s: ", hitId));
				// System.out.print(String.format("%.2f, %.2f\n",
				// actualAvgPayoff, simulatedAvgPayoff));
			}
		}
	
		writer.flush();
		writer.close();
	
		System.out.println("diff average " + diffTotal * 1.0 / count);
	
		BufferedWriter writerMatlab = new BufferedWriter(new FileWriter(rootDir
				+ type + "PairedTTest.m"));
		writerMatlab.write("actual = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ",
						game.actualPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
	
		writerMatlab.write("simulated = [");
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				writerMatlab.write(String.format("%.2f ",
						game.simulatedFPPayoff.get(hitId)));
			}
		}
		writerMatlab.write("];\n");
		writerMatlab.write("[h,p] = ttest(actual,simulated)\n");
		writerMatlab.write("diff = simulated - actual;\n");
		writerMatlab.write("m = mean(diff);\n" + "v = std(diff);\n"
				+ "diff2 = (diff - m)/v;\n" + "h2=kstest(diff2)\n"
				+ "[f,x_values] = ecdf(diff2);\n" + "F = plot(x_values,f);\n"
				+ "set(F,'LineWidth',2);\n" + "hold on;\n"
				+ "G = plot(x_values,normcdf(x_values,0,1),'r-');\n"
				+ "set(G,'LineWidth',2);\n");
		writerMatlab.flush();
		writerMatlab.close();
	
	}

	private static void noRegretLearningAnalysis() {
	
		List<Map<String, Map<String, Double>>> experts = new ArrayList<Map<String, Map<String, Double>>>();
		double unit = 0.25;
		int numUnit = Math.round((int) (1 / unit));
		for (int i = 0; i <= numUnit; i++) {
			for (int j = 0; j <= numUnit; j++) {
				double probMMGivenMM = i * unit;
				double probMMGivenGB = j * unit;
	
				Map<String, Map<String, Double>> strategy = new HashMap<String, Map<String, Double>>();
	
				Map<String, Double> mmStrategy = new HashMap<String, Double>();
				mmStrategy.put("MM", probMMGivenMM);
				mmStrategy.put("GB", 1 - probMMGivenMM);
				Map<String, Double> gbStrategy = new HashMap<String, Double>();
				gbStrategy.put("MM", probMMGivenGB);
				gbStrategy.put("GB", 1 - probMMGivenGB);
	
				strategy.put("MM", mmStrategy);
				strategy.put("GB", mmStrategy);
	
				experts.add(strategy);
			}
		}
		System.out.printf("number of experts %d\n", experts.size());
	
		// Put in initial weights
		List<List<Double>> weights = new ArrayList<List<Double>>();
		List<Double> initWeights = new ArrayList<Double>();
		for (int i = 0; i < experts.size(); i++) {
			initWeights.add(1.0);
		}
		weights.add(initWeights);
	
		for (Game game : expSet.games) {
			// Game game = expSet.games.get(10);
	
			for (String hitId : game.playerHitIds) {
				// String hitId = game.playerHitIds[0];
	
				System.out.printf("actual payoff %.2f\n",
						game.actualPayoff.get(hitId));
	
				for (int i = 0; i < expSet.numRounds; i++) {
	
					// Calculate payoffs
					List<Double> expertPayoffs = new ArrayList<Double>();
					for (int j = 0; j < experts.size(); j++) {
	
						if (treatment.equals("prior2-uniquetruthful")) {
							double currPayoff = getPayoffT3(hitId, game, i,
									experts.get(j));
							expertPayoffs.add(currPayoff);
						} else {
	
						}
					}
	
					// Calculate player payoff
					double playerPayoff = 0.0;
					double totalWeight = 0.0;
					for (int j = 0; j < experts.size(); j++) {
						totalWeight += weights.get(i).get(j);
					}
					for (int j = 0; j < experts.size(); j++) {
						playerPayoff += weights.get(i).get(j) / totalWeight
								* expertPayoffs.get(j);
					}
					System.out.printf("round %s: my payoff %.2f\n", i,
							playerPayoff);
	
					// Update weights
					List<Double> currWeights = new ArrayList<Double>();
					for (int j = 0; j < experts.size(); j++) {
						double weight = weights.get(i).get(j);
						double newWeight = weight
								* (1 + 0.1 * expertPayoffs.get(j));
						currWeights.add(newWeight);
					}
					weights.add(currWeights);
					// System.out.printf("round %s: %s\n", i,
					// currWeights.toString());
	
				}
	
			}
		}
	}

	private static void mixedStrategyPayoff(String rule) {
		if (!treatment.equals("prior2-uniquetruthful"))
			return;
	
		// double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule,
		// priorProbs, prior);
		// System.out.printf("Payoff at truthful equilibrium: %.6f\n",
		// truthfulPayoffT3);
		//
		// double mixedPayoffT3 = AnalysisUtils.getMixedPayoff(rule, priorProbs,
		// prior, strategyMMGivenMM, strategyMMGivenGB);
	}

	private static void strategyChangeT1() {
		if (!treatment.equals("prior2-basic"))
			return;
	
		int numMMEq = 0;
		int numMixedToMM = 0;
		int numTruthfulToMM = 0;
		for (Game game : expSet.games) {
	
			boolean convergedToMM = true;
			for (String hitId : game.playerHitIds) {
				if (game.stateSeq.get(hitId)[expSet.numRounds - 1] != mmState) {
					convergedToMM = false;
					break;
				}
			}
	
			if (!convergedToMM)
				continue;
	
			numMMEq++;
			for (String hitId : game.playerHitIds) {
				int[] stateSeq = game.stateSeq.get(hitId);
				if (hasMixedAndMM(stateSeq))
					numMixedToMM++;
				else if (hasTruthfulAndMM(stateSeq))
					numTruthfulToMM++;
			}
		}
		System.out.printf("MM eq : %d, mixed to MM: %d, truthful to MM: %d\n",
				numMMEq, numMixedToMM, numTruthfulToMM);
	
	}

	private static boolean hasTruthfulAndMM(int[] stateSeq) {
		boolean hasTruthful = false;
		for (int i = 0; i < stateSeq.length; i++) {
			if (stateSeq[i] == truthfulState) {
				hasTruthful = true;
			} else if (stateSeq[i] == mmState) {
			} else {
				return false;
			}
		}
		return hasTruthful;
	}

	private static boolean hasMixedAndMM(int[] stateSeq) {
		boolean hasMixed = false;
		for (int i = 0; i < stateSeq.length; i++) {
			if (stateSeq[i] == mixedState) {
				hasMixed = true;
			} else if (stateSeq[i] == mmState) {
			} else {
				return false;
			}
		}
		return hasMixed;
	}

	// private static void strategyPayoffAnalysis(String rule) {
	//
	// System.out.println("\n" + "Strategy payoff analysis -- " + rule);
	//
	// double truthfulPayoffT3 = AnalysisUtils.getTruthfulPayoff(rule,
	// priorProbs, prior);
	// System.out.printf("Payoff at truthful equilibrium: %.6f\n",
	// truthfulPayoffT3);
	//
	// double mixedPayoff = 0.0;
	// double[] strategy = new double[2];
	//
	// double unit = 0.1;
	// int numUnit = Math.round((int)(1 / unit));
	//
	// for (int i = 0; i <= numUnit; i++) {
	// for (int j = 0; j <= numUnit; j++) {
	// double strategyMMGivenMM = unit * i;
	// double strategyMMGivenGB = unit * j;
	// double mixedPayoffT5 = AnalysisUtils.getMixedPayoff(rule,
	// priorProbs, prior, strategyMMGivenMM, strategyMMGivenGB);
	// // System.out.printf("mixed strategy is (%.2f, %.2f), payoff is %.6f\n",
	// // strategyMMGivenMM, strategyMMGivenGB, mixedPayoffT5);
	// if (mixedPayoffT5 > mixedPayoff) {
	// mixedPayoff = mixedPayoffT5;
	// strategy[0] = strategyMMGivenMM;
	// strategy[1] = strategyMMGivenGB;
	// }
	// }
	// }
	// System.out.printf("best mixed strategy; (%.2f, %.2f), best payoff; %.6f",
	// strategy[0], strategy[1], mixedPayoff);
	// }
	
	private static void strategyPayoffAnalysis(String treatment) {
	
		System.out.println("\n" + "Strategy payoff analysis");
	
		double truthfulPayoffT3 = Utils.getTruthfulPayoff(treatment,
				expSet.priorProbs, expSet.worlds);
		System.out.printf("Payoff at truthful equilibrium: %.6f\n",
				truthfulPayoffT3);
	
		// Payoff of mixed strategies
		if (mixedState == -1) {
			System.out
					.printf("HMM did not learn a mixed strategy.  Do not analyze its payoff");
		} else {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdfMixed = learntHmm
					.getOpdf(mixedState);
			System.out.printf("Mixed strategy: %s", opdfMixed.toString());
			double probMMGivenMM = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double probMMGivenGB = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			double mixedPayoffT3 = Utils.getMixedPayoff(treatment,
					expSet.priorProbs, expSet.worlds, probMMGivenMM,
					probMMGivenGB);
			System.out.printf(
					"Payoff with symmetric mixed strategies:  %.6f\n",
					mixedPayoffT3);
	
			// Average of payoffs for players using mixed strategy by the end
			int count = 0;
			double totalPayoff = 0.0;
			for (Game game : expSet.games) {
				for (String hitId : game.playerHitIds) {
					if (game.stateSeq.get(hitId)[expSet.numRounds - 1] == mixedState) {
						count++;
						totalPayoff += game.actualPayoff.get(hitId);
					}
				}
			}
			double avgPayoff = totalPayoff / count;
			System.out.printf("Average of payoffs for mixed strategy:  %.6f\n",
					avgPayoff);
		}
	
		if (mixed2State == -1) {
			System.out
					.printf("HMM did not learn a mixed 2 strategy.  Do not analyze its payoff");
		} else {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdfMixed = learntHmm
					.getOpdf(mixed2State);
			System.out.printf("Mixed strategy: %s", opdfMixed.toString());
			double probMMGivenMM = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.MM, CandyReport.MM));
			double probMMGivenGB = opdfMixed
					.probability(new SigActObservation<CandySignal, CandyReport>(
							CandySignal.GB, CandyReport.MM));
			double mixedPayoffT3 = Utils.getMixedPayoff(treatment,
					expSet.priorProbs, expSet.worlds, probMMGivenMM,
					probMMGivenGB);
			System.out.printf(
					"Payoff with symmetric mixed strategies:  %.6f\n",
					mixedPayoffT3);
	
			// Average of payoffs for players using mixed strategy by the end
			int count = 0;
			double totalPayoff = 0.0;
			for (Game game : expSet.games) {
				for (String hitId : game.playerHitIds) {
					if (game.stateSeq.get(hitId)[expSet.numRounds - 1] == mixed2State) {
						count++;
						totalPayoff += game.actualPayoff.get(hitId);
					}
				}
			}
			double avgPayoff = totalPayoff / count;
			System.out.printf(
					"Average of payoffs for mixed 2 strategy:  %.6f\n",
					avgPayoff);
		}
	
	}

	private static double getPayoffT3(String hitId, Game game, int i,
			Map<String, Map<String, Double>> expert) {
	
		Round currRound = game.rounds.get(i);
		String signal = currRound.getSignal(hitId);
		Map<String, Double> strategyForSignal = expert.get(signal);
		List<String> otherReports = game.getOtherReportList(currRound, hitId);
	
		int numMMInOtherReports = Utils.getNumMMInRefReports(otherReports);
	
		return strategyForSignal.get("MM")
				* Utils.getPaymentTreatmentUniqueTruthful("MM",
						numMMInOtherReports)
				+ strategyForSignal.get("GB")
				* Utils.getPaymentTreatmentUniqueTruthful("GB",
						numMMInOtherReports);
	}

	private static double getPayoffT3(Map<String, Double> strategyForSignal,
			int numMMInOtherReports) {
		return strategyForSignal.get("MM")
				* Utils.getPaymentTreatmentUniqueTruthful("MM",
						numMMInOtherReports)
				+ strategyForSignal.get("GB")
				* Utils.getPaymentTreatmentUniqueTruthful("GB",
						numMMInOtherReports);
	}

	// helper
	static List<List<SigActObservation<CandySignal, CandyReport>>> getActObsSequence() {
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = new ArrayList<List<SigActObservation<CandySignal, CandyReport>>>();
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> list = game.signalReportObjList
						.get(hitId);
				seq.add(list);
			}
		}
		return seq;
	}

}
