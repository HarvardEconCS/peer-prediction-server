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
import java.util.Random;
import java.util.Scanner;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.BWToleranceLearner;
import net.andrewmao.models.games.OpdfStrategy;
import net.andrewmao.models.games.SigActObservation;

import org.apache.commons.math3.analysis.MultivariateFunction;
import org.apache.commons.math3.linear.Array2DRowRealMatrix;
import org.apache.commons.math3.linear.RealMatrix;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.MaxEval;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.BOBYQAOptimizer;
import org.apache.commons.math3.stat.descriptive.DescriptiveStatistics;

import be.ac.ulg.montefiore.run.jahmm.Hmm;
import be.ac.ulg.montefiore.run.jahmm.Opdf;
import be.ac.ulg.montefiore.run.jahmm.ViterbiCalculator;

import com.cureos.numerics.Calcfc;
import com.cureos.numerics.Cobyla;
import com.google.common.collect.ImmutableMap;

public class LogReader {

	static String dbUrl = "jdbc:mysql://localhost/peerprediction";
	static String dbClass = "com.mysql.jdbc.Driver";
	static String setId = "vary-payment";
	 static String treatment = "prior2-basic";
//	 static String treatment = "prior2-outputagreement";
//	 static String treatment = "prior2-uniquetruthful";
//	 static String treatment = "prior2-symmlowpay";
	// static String treatment = "prior2-constant";

	static final String rootDir = "/Users/alicexigao/Dropbox/peer-prediction/data/"
			+ treatment + "/";

	static Experiment expSet;

	// For HMM estimation
	static int numStrategies = -1;
	static String[] strategyNames = null;
	static int numRestarts = 1;
	static int numHmmStates = 4;
	static double tol = 0.02;
	static Hmm<SigActObservation<CandySignal, CandyReport>> learntHmm = null;
	static int mmState = -1;
	static int gbState = -1;
	static int truthfulState = -1;
	static int mixedState = -1;
	static int mixed2State = -1;

	static String[] signalList = new String[] { "MM", "GB" };

	static double[] meanloglk = new double[4];
	static double[] uppers = new double[4];
	static double[] lowers = new double[4];

	static Map<String, double[]> avgLogLks;
	static double randomLogLk;

	static int numRoundsCV = 10;
	static int numFolds = 10;

	static Random rand = new Random();

	public static void main(String[] args) throws Exception {

		File dir = new File(rootDir);
		dir.mkdirs();

		// Parsing game log and exit survey, and print info
		parseLog();
		writePlayerComments();
		printInfo();

		// lazyStrategy(); // check whether player always chooses left or right
		// choice

		// Statistics of raw data
		// writeRawDataToFile();
		// graphRawData();
		// calcAvgBonus();

		// HMM analysis
		// learnHMM();
		// setStrategyNames();
		// writeStateSeq();
		// eqConvergenceHmm();
		// writeStrategyChangeHeatMap();

		// writeStrategyDistribution();
		// genPredictedStrategyChangeCode();
		// graphLogLikelihood();

		// Simple analysis
		// eqConvergenceSimpleMethod();

		// Learning analysis
		avgLogLks = new HashMap<String, double[]>();
		randomLogLk = getLogLkRandomModel();
//		getPredictiveLogLk("HMM");
//		getPredictiveLogLk("RLS");
//		getPredictiveLogLk("SFPS");
//		getPredictiveLogLk("S1");
		getPredictiveLogLk("S2");
		// getPredictiveLogLk("EWAS");
		
		// getPredictiveLogLk("EWANS");
		// getPredictiveLogLk("SFPNS"); // not best in all treatments
		// getPredictiveLogLk("RLNS"); // definitely worse for all treatments
		graphPredictiveLogLk();

	}

	public static void getPredictiveLogLk(String model) throws IOException {
		System.out.println("Get predictive likelihood for " + model);

		int groupSize = expSet.games.size() / numFolds;
		double[] loglks = new double[numRoundsCV];
		DescriptiveStatistics stats = new DescriptiveStatistics();

		for (int l = 0; l < numRoundsCV; l++) {

			System.out.printf("Round %d: ", l);
			stats.clear();

			Collections.shuffle(expSet.games);

			System.out.print("Folds ");
			for (int i = 0; i < numFolds; i++) {

				System.out.printf("%d ", i);
				 System.out.println();

				// Divide up data into test and training sets
				List<Game> testSet = new ArrayList<Game>();
				List<Game> trainingSet = new ArrayList<Game>();
				int testStart = i * groupSize;
				for (int j = 0; j < numFolds * groupSize; j++) {
					if (j >= testStart && j < testStart + groupSize) {
						testSet.add(expSet.games.get(j));
					} else {
						trainingSet.add(expSet.games.get(j));
					}
				}

				// Estimate best parameters on training set
				Map<String, Object> bestParam = estimateParameters(model, trainingSet);
				 System.out.println(bestParam.toString());

				// Compute loglk on test set
				double testLoglk = getTestLogLk(model, bestParam, testSet);
				stats.addValue(testLoglk);
			}

			loglks[l] = stats.getMean() - randomLogLk;

			System.out.printf(" avgloglk = %.2f", loglks[l]);
			System.out.println();
			
			System.exit(0);
		}

		stats.clear();
		for (int i = 0; i < loglks.length; i++) {
			stats.addValue(loglks[i]);
		}
		double stdev = stats.getStandardDeviation();
		double stdError = stdev / Math.sqrt(numRoundsCV);
		System.out.printf("avgloglk in [%.2f, %.2f]\n", stats.getMean() - 1.96
				* stdError, stats.getMean() + 1.96 * stdError);
		avgLogLks.put(model, loglks);
	}

	public static void graphPredictiveLogLk() throws IOException {
	
		System.out.println("Graphing distributions of log likelihoods");
		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "predictiveLogLk.m"));
	
		int numModels = avgLogLks.keySet().size();
		List<String> modelNames = new ArrayList<String>();
		double[] displayMeans = new double[numModels];
		double[] displayErrors = new double[numModels];
		int index = 0;
	
		DescriptiveStatistics stats = new DescriptiveStatistics();
	
		modelNames.addAll(avgLogLks.keySet());
		Collections.sort(modelNames);
		for (int i = 0; i < modelNames.size(); i++) {
			String model = modelNames.get(i);
			stats.clear();
			double[] ll = avgLogLks.get(model);
			for (int j = 0; j < ll.length; j++) {
				stats.addValue(ll[j]);
			}
			displayMeans[index] = stats.getMean();
			double stdev = stats.getStandardDeviation();
			double stdError = stdev / Math.sqrt(numRoundsCV);
			displayErrors[index] = 1.96 * stdError;
			index++;
		}
	
		StringBuilder sb = new StringBuilder();
		sb.append(treatment);
		sb.append("\n");
		for (int i = 0; i < numModels; i++) {
			sb.append(String.format("'%s',", modelNames.get(i)));
		}
		sb.append("\n");
		sb.append(String.format("mean = ["));
		for (int i = 0; i < numModels; i++) {
			sb.append(String.format("%.2f ", displayMeans[i]));
		}
		sb.append(String.format("];\n" + "error = ["));
		for (int i = 0; i < numModels; i++) {
			sb.append(String.format("%.2f ", displayErrors[i]));
		}
		sb.append(String.format("];\n"));
		writer.write(sb.toString());
	
		// writer.write("figure(1);\n"
		// + "clf;\n"
		// + "hold('on');\n"
		// + "bar( x, t1 );\n"
		// + "errorbar( x, t1, ci );\n"
		// + "xlh = xlabel('Learning Model');\n"
		// + "ylh = ylabel('Log Likelihood');\n"
		// + "set(xlh, 'FontSize', 26);\n"
		// + "set(ylh, 'FontSize', 26);\n"
		// + "axes = findobj(gcf,'type','axes');\n"
		// + "set(axes, 'FontSize', 20);\n"
		// + "set(gca, 'XLim', [0.5 3.5]);\n"
		// + "set(gca, 'XTick', 1:3);\n"
		// + "set(gca,'XTickLabel',{'HMM', 'RL', 'SFP'});");
	
		writer.flush();
		writer.close();
	
	}

	private static double getLogLkRandomModel() {
		int groupSize = expSet.games.size() / 10;
		double randomLogLk = Math.log(0.5) * groupSize * expSet.numRounds
				* expSet.numPlayers;
		return randomLogLk;
	}

	private static Map<String, Object> estimateParameters(String model,
			List<Game> trainingSet) {

		if (model.equals("HMM")) {

			return estimateHMM(trainingSet);

		} else if (model.equals("RLS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsRLorSFP(), "RLS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("phi", point[0]);
			params.put("lambda", point[1]);
			params.put("considerSignal", true);
			return params;

		} else if (model.equals("RLNS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsRLorSFP(), "RLNS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("phi", point[0]);
			params.put("lambda", point[1]);
			params.put("considerSignal", false);
			return params;

		} else if (model.equals("SFPS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsRLorSFP(), "SFPS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("rho", point[0]);
			params.put("lambda", point[1]);
			params.put("considerSignal", true);
			return params;

		} else if (model.equals("SFPNS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsRLorSFP(), "SFPNS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("rho", point[0]);
			params.put("lambda", point[1]);
			params.put("considerSignal", false);
			return params;

		} else if (model.equals("EWAS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsEWA(), "EWAS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("rho", point[0]);
			params.put("phi", point[1]);
			params.put("delta", point[2]);
			params.put("lambda", point[3]);
			params.put("considerSignal", true);
			return params;

		} else if (model.equals("EWANS")) {

			double[] point = estimateUsingApacheOptimizer(trainingSet, getBoundsEWA(), "EWANS");

			Map<String, Object> params = new HashMap<String, Object>();
			params.put("rho", point[0]);
			params.put("phi", point[1]);
			params.put("delta", point[2]);
			params.put("lambda", point[3]);
			params.put("considerSignal", false);
			return params;

		} else if (model.startsWith("S1")) {

			double bestSwitchRound = -1.0;
			double[] bestPoint = null;
			double bestLogLk = Double.NEGATIVE_INFINITY;
			
			for (int k = 0; k < expSet.numRounds + 1; k++) {
				
				String subModel = String.format("S1-%d", k);
				double[] tempPoint = estimateUsingCobyla(trainingSet, subModel);
				
				Map<String, Object> params = new HashMap<String, Object>();
				params.put("diffThreshold", tempPoint[0]);
				params.put("probTruthful", 	tempPoint[1]);
				params.put("probMM", 		tempPoint[2]);
				params.put("probGB", 		tempPoint[3]);
				params.put("switchRound", k * 1.0);
				
				double loglk = LogReader.computeLogLkS1(params, trainingSet);
				
				if (loglk > bestLogLk) {
					bestPoint = tempPoint;
					bestSwitchRound = k;
				}
			}
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("diffThreshold", bestPoint[0]);
			params.put("probTruthful", 	bestPoint[1]);
			params.put("probMM", 		bestPoint[2]);
			params.put("probGB", 		bestPoint[3]);
			params.put("switchRound", 	bestSwitchRound);
			return params;
			
		} else if (model.equals("S2")) {
			
			double[] point = estimateUsingCobyla(trainingSet, "S2");
			
			Map<String, Object> params = new HashMap<String, Object>();
			params.put("probTruthful", 	point[0]);
			params.put("probMM", 		point[1]);
			params.put("probGB", 		point[2]);
			return params;
		}

		return null;
	}

	private static double getTestLogLk(String model,
			Map<String, Object> bestParam, List<Game> testSet) {
	
		if (model.equals("HMM")) {
	
			@SuppressWarnings("unchecked")
			Hmm<SigActObservation<CandySignal, CandyReport>> bestHmm = 
				(Hmm<SigActObservation<CandySignal, CandyReport>>) bestParam.get("HMM");
			List<List<SigActObservation<CandySignal, CandyReport>>> seq = 
					LogReader.getActObsSequence(testSet);
			return BWToleranceLearner.computeLogLk(bestHmm, seq);
	
		} else if (model.startsWith("RL")) {
	
			return computeLogLkRL(bestParam, testSet);
	
		} else if (model.startsWith("SFP")) {
	
			return computeLogLkSFP(bestParam, testSet);
	
		} else if (model.startsWith("EWA")) {
	
			return computeLogLkEWA(bestParam, testSet);
	
		} else if (model.startsWith("S1")) {
	
			return computeLogLkS1(bestParam, testSet);
	
		} else if (model.startsWith("S2")) {
		
			return computeLogLkS2(bestParam, testSet);

		}
		
		return 0;
	}

	static double[] estimateUsingCobyla(List<Game> trainingSet, String model) {

		double rhobeg = 0.5;
		double rhoend = Utils.eps;
		int iprint = 0;
		int maxfun = 3500;
		int numVariables = 0;
		int numConstraints = 0;
		double[] point = null;

		if (model.startsWith("SFP") || model.startsWith("RL")) {
			
			numVariables = 2;
			numConstraints = 4;
			point = new double[]{0.0, 1.0};
			
		} else if (model.startsWith("S1")) {
			
			numVariables = 4;
			numConstraints = 9;
			point = getRandomStartPointS1();
			
		} else if (model.startsWith("S2")) {
			
			numVariables = 3;
			numConstraints = 7;
			point = getRandomStartPointS2();
			
		}

		Calcfc function = new LogLkFunctionCobyla(trainingSet, model);
		Cobyla.FindMinimum(function, numVariables, numConstraints, point,
				rhobeg, rhoend, iprint, maxfun);
		return point;
	}

	static double[] estimateUsingApacheOptimizer(List<Game> games,
			Map<String, Object> bounds, String model) {

		MultivariateFunction function = new LogLkFunctionApache(games, model);
		double[] lb = (double[]) bounds.get("lb");
		double[] ub = (double[]) bounds.get("ub");

		BOBYQAOptimizer optimizer = new BOBYQAOptimizer(lb.length + 2);
		double[] startPoint = (double[]) bounds.get("lb");

		PointValuePair optimum = optimizer.optimize(new MaxEval(10000000),
				new ObjectiveFunction(function), GoalType.MAXIMIZE,
				new InitialGuess(startPoint), new SimpleBounds(lb, ub));

		double[] point = optimum.getPoint();
		return point;
	}

	public static Map<String, Object> estimateHMM(List<Game> trainingSet) {
		Hmm<SigActObservation<CandySignal, CandyReport>> bestHmm = null;
		try {
			bestHmm = LogReader.learnHMM(trainingSet, numHmmStates, numRestarts);
		} catch (IOException e) {
			e.printStackTrace();
		}
		Map<String, Object> bestParam = new HashMap<String, Object>();
		bestParam.put("HMM", bestHmm);
		return bestParam;
	}

	private static Map<String, Object> getBoundsRLorSFP() {
		Map<String, Object> bounds = new HashMap<String, Object>();
		bounds.put("lb", new double[] { 0, 1 });
		bounds.put("ub", new double[] { 1, 10 });
		return bounds;
	}

	private static Map<String, Object> getBoundsEWA() {
		Map<String, Object> bounds = new HashMap<String, Object>();
		bounds.put("lb", new double[] { 0, 0, 0, 1 });
		bounds.put("ub", new double[] { 1, 1, 1, 10 });
		return bounds;
	}

	private static double[] getRandomStartPointS1() {
		double diffThresholdStart = rand.nextDouble();
		double[] randomVec = Utils.getRandomVec(4);
		return new double[] { diffThresholdStart, randomVec[0], randomVec[1], randomVec[2]};
	}
	
	private static double[] getRandomStartPointS2() {
		double[] randomVec = Utils.getRandomVec(4);
		return new double[] {randomVec[0], randomVec[1], randomVec[2]};
	}

	public static double computeLogLkRL(Map<String, Object> bestParam,
			List<Game> games) {

		boolean considerSignal = (boolean) bestParam.get("considerSignal");
		double phi = (double) bestParam.get("phi");
		double lambda = (double) bestParam.get("lambda");

		double firstRoundMMProb = 0.5;
		double loglk = 0;
		for (Game game : games) {

			double logLkGame = 0;

			// initialize attraction
			Map<String, Map<Pair<String, String>, Double>> attraction = 
					initAttraction(game.playerHitIds);

			for (int i = 0; i < expSet.numRounds; i++) {

				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;

				// if first two treatments, add LogLk for reward except last
				// round
				if (treatment.equals("prior2-basic")
						|| treatment.equals("prior2-outputagreement")) {

					if (i != expSet.numRounds - 1) {

						for (String currPlayerId : game.playerHitIds) {

							int numRefReport = getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numRefReport * 1.0
									/ (expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}

					}

				}

				// add LogLk for report
				if (i == 0) {

					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							expSet.numPlayers));

				} else {

					Map<String, Map<String, Object>> resultPrevRound = game.rounds
							.get(i - 1).result;

					for (String playerId : game.playerHitIds) {

						String signalCurrRound = (String) resultCurrRound.get(
								playerId).get("signal");
						String reportCurrRound = (String) resultCurrRound.get(
								playerId).get("report");
						String signalPrevRound = (String) resultPrevRound.get(
								playerId).get("signal");
						String reportPrevRound = (String) resultPrevRound.get(
								playerId).get("report");
						double rewardPrevRound = (double) resultPrevRound.get(
								playerId).get("reward");

						// update attractions
						updateAttractionsRL(attraction, playerId, phi,
								signalPrevRound, reportPrevRound,
								rewardPrevRound);

						// determine strategy
						Map<String, Double> strategy = getStrategy(attraction,
								playerId, considerSignal, lambda,
								signalCurrRound, signalPrevRound);

						// get loglk for report
						logLkGame += getLogLkForReport(strategy,
								reportCurrRound, playerId);

					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	public static double computeLogLkSFP(Map<String, Object> bestParam,
			List<Game> games) {

		boolean considerSignal = (boolean) bestParam.get("considerSignal");
		double rho = (double) bestParam.get("rho");
		double lambda = (double) bestParam.get("lambda");

		double loglk = 0;
		double firstRoundMMProb = 0.5;
		for (Game game : games) {

			double logLkGame = 0;

			// initialize
			double experiences = Utils.eps;
			Map<String, Map<Pair<String, String>, Double>> attractions = initAttraction(game.playerHitIds);

			for (int i = 0; i < expSet.numRounds; i++) {

				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;

				if (treatment.equals("prior2-basic")
						|| treatment.equals("prior2-outputagreement")) {

					// add LogLk for reward except last round
					if (i != expSet.numRounds - 1) {

						for (String currPlayerId : game.playerHitIds) {

							/**
							 * Given the reward of the current player, get
							 * number of other players that could possibly have
							 * been the reference player.
							 */
							int numPossibleRefPlayers = getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numPossibleRefPlayers
									* 1.0 / (expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}

					}

				}

				if (i == 0) {

					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							expSet.numPlayers));

				} else {

					Map<String, Map<String, Object>> resultPrevRound = game.rounds
							.get(i - 1).result;

					for (String playerId : game.playerHitIds) {

						String signalPrev = (String) resultPrevRound.get(
								playerId).get("signal");
						String reportPrev = (String) resultPrevRound.get(
								playerId).get("report");
						double rewardPrev = (double) resultPrevRound.get(
								playerId).get("reward");
						String signalCurr = (String) resultCurrRound.get(
								playerId).get("signal");
						String reportCurr = (String) resultCurrRound.get(
								playerId).get("report");
						int numOtherMMReportsPrev = Utils.getNumOfGivenReport(
								resultPrevRound, "MM", playerId);

						// update attractions
						updateAttractionsSFP(attractions, experiences,
								playerId, rho, reportPrev, rewardPrev,
								numOtherMMReportsPrev);

						// update experiences
						experiences = updateExperience(experiences, rho);

						// get strategy
						Map<String, Double> strategy = getStrategy(attractions,
								playerId, considerSignal, lambda, signalCurr,
								signalPrev);

						// add loglk for report
						logLkGame += getLogLkForReport(strategy, reportCurr,
								playerId);

					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	public static double computeLogLkEWA(Map<String, Object> bestParam,
			List<Game> games) {

		boolean considerSignal = (boolean) bestParam.get("considerSignal");
		double rho = (double) bestParam.get("rho"); // experience discount
													// factor
		double phi = (double) bestParam.get("phi"); // attraction discount
													// factor
		double delta = (double) bestParam.get("delta"); // weight for
														// hypothetical payoffs
		double lambda = (double) bestParam.get("lambda"); // sensitivity
															// parameter

		double firstRoundMMProb = 0.5;
		double loglk = 0;
		for (Game game : games) {

			double logLkGame = 0;

			// initialize experience and attractions
			double experience = Utils.eps;
			Map<String, Map<Pair<String, String>, Double>> attractions = initAttraction(game.playerHitIds);

			for (int i = 0; i < expSet.numRounds; i++) {

				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;

				if (treatment.equals("prior2-basic")
						|| treatment.equals("prior2-outputagreement")) {

					// add LogLk for reward except last round
					if (i != expSet.numRounds - 1) {

						for (String currPlayerId : game.playerHitIds) {

							/**
							 * Given the reward of the current player, get
							 * number of other players that could possibly have
							 * been the reference player.
							 */
							int numPossibleRefPlayers = getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numPossibleRefPlayers
									* 1.0 / (expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}

					}

				}

				// add LogLk for report
				if (i == 0) {

					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							expSet.numPlayers));

				} else {

					Map<String, Map<String, Object>> resultPrevRound = game.rounds
							.get(i - 1).result;

					for (String playerId : game.playerHitIds) {

						String reportCurrRound = (String) resultCurrRound.get(
								playerId).get("report");
						String signalCurrRound = (String) resultCurrRound.get(
								playerId).get("signal");
						String signalPrevRound = (String) resultPrevRound.get(
								playerId).get("signal");
						String reportPrevRound = (String) resultPrevRound.get(
								playerId).get("report");
						double rewardPrevRound = (double) resultPrevRound.get(
								playerId).get("reward");
						int numOtherMMReportsPrev = Utils.getNumOfGivenReport(
								resultPrevRound, "MM", playerId);

						// update attractions
						updateAttractionsEWA(attractions, experience, playerId,
								rho, delta, phi, signalPrevRound,
								reportPrevRound, rewardPrevRound,
								reportCurrRound, numOtherMMReportsPrev);

						// update experiences
						experience = updateExperience(experience, rho);

						// get strategy
						Map<String, Double> strategy = getStrategy(attractions,
								playerId, considerSignal, lambda,
								signalCurrRound, signalPrevRound);

						// add loglk for report
						logLkGame += getLogLkForReport(strategy,
								reportCurrRound, playerId);
					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	public static double computeLogLkS1(Map<String, Object> params,
			List<Game> games) {

		int k = (int) Math.round((double) params.get("switchRound"));
		double diffThreshold = (double) params.get("diffThreshold");
		double probTruthful = (double) params.get("probTruthful");
		double probMM = (double) params.get("probMM");
		double probGB = (double) params.get("probGB");
		double probRandom = 1 - probTruthful - probMM - probGB;

		double loglk = 0;

		for (Game game : games) {

			for (String playerId : game.playerHitIds) {

				double lkPlayer = 0;

				// Before switch, may use Truthful or Random strategies
				double lkTruthful = probTruthful * getLkTruthful(0, k, game, playerId);
				double lkMMStart = probMM * getLkReport(0, k, game, playerId, "MM");
				double lkGBStart = probGB * getLkReport(0, k, game, playerId, "GB");
				double lkRandom = probRandom * Math.pow(0.5, k);

				double countTotal = k * expSet.numPlayers;
				double countMM = Utils.getNumMMReports(0, k - 1, game, playerId);
				double percentMM = countMM / countTotal;
				double percentGB = 1 - percentMM;

				if (percentMM - percentGB >= diffThreshold) {

					// Switch to MM strategy
					lkPlayer = (lkTruthful + lkMMStart + lkGBStart + lkRandom)
							* getLkReport(k, expSet.numRounds, game, playerId, "MM");
					

				} else if (percentGB - percentMM >= diffThreshold) {

					// Switch to GB strategy
					lkPlayer = (lkTruthful + lkMMStart + lkGBStart + lkRandom)
							* getLkReport(k, expSet.numRounds, game, playerId, "GB");

				} else {

					// Stay with original strategy
					lkPlayer = probTruthful * getLkTruthful(0, expSet.numRounds, game, playerId)
							+ probMM * getLkReport(0, expSet.numRounds, game, playerId, "MM")
							+ probGB * getLkReport(0, expSet.numRounds, game, playerId, "GB") 
							+ probRandom * Math.pow(0.5, expSet.numRounds);

				}

				loglk += Math.log(lkPlayer);
			}
		}

		return loglk;
	}

	public static double computeLogLkS2(Map<String, Object> params,
			List<Game> games) {

		double probTruthful = (double) params.get("probTruthful");
		double probMM = (double) params.get("probMM");
		double probGB = (double) params.get("probGB");
		double probRandom = 1 - probTruthful - probMM - probGB;

		double loglk = 0;

		for (Game game : games) {

			for (String playerId : game.playerHitIds) {

				double lkPlayer = probTruthful * getLkTruthful(0, expSet.numRounds, game, playerId)
							+ probMM * getLkReport(0, expSet.numRounds, game, playerId, "MM")
							+ probGB * getLkReport(0, expSet.numRounds, game, playerId, "GB") 
							+ probRandom * Math.pow(0.5, expSet.numRounds);

				loglk += Math.log(lkPlayer);
			}
		}

		return loglk;
	}

	static double getLkReport(int roundStart, int roundEnd, Game game,
			String playerId, String givenReport) {
		double lk = 1;
		for (int i = roundStart; i < roundEnd; i++) {
			String report = game.rounds.get(i).getReport(playerId);
			if (report.equals(givenReport))
				lk *= 1 - Utils.eps;
			else
				lk *= Utils.eps;
		}
		return lk;
	}

	static double getLkTruthful(int roundStart, int roundEnd, Game game, String playerId) {
		double lk = 1;
		for (int i = roundStart; i < roundEnd; i++) {
			String signal = game.rounds.get(i).getSignal(playerId);
			String report = game.rounds.get(i).getReport(playerId);
			if (signal.equals(report))
				lk *= 1 - Utils.eps;
			else
				lk *= Utils.eps;
		}
		return lk;
	}

	public static Map<String, Map<Pair<String, String>, Double>> initAttraction(
			String[] playerHitIds) {
		Map<String, Map<Pair<String, String>, Double>> attraction = 
				new HashMap<String, Map<Pair<String, String>, Double>>();
		for (String player : playerHitIds) {
			Map<Pair<String, String>, Double> payoffs = new HashMap<Pair<String, String>, Double>();
			payoffs.put(new Pair<String, String>("MM", "MM"), 0.0);
			payoffs.put(new Pair<String, String>("MM", "GB"), 0.0);
			payoffs.put(new Pair<String, String>("GB", "MM"), 0.0);
			payoffs.put(new Pair<String, String>("GB", "GB"), 0.0);
			attraction.put(player, payoffs);
		}
		return attraction;
	}

	public static void updateAttractionsRL(
			Map<String, Map<Pair<String, String>, Double>> attraction,
			String playerId, double phi, String signalPrev, String reportPrev,
			double rewardPrev) {
	
		Map<Pair<String, String>, Double> playerAttraction = attraction
				.get(playerId);
	
		for (Pair<String, String> key : playerAttraction.keySet()) {
			if (key.t2.equals(reportPrev)) {
				double newAttr = phi * playerAttraction.get(key) + rewardPrev;
				playerAttraction.put(key, newAttr);
			} else {
				double newAttr = phi * playerAttraction.get(key);
				playerAttraction.put(key, newAttr);
			}
		}
	
		attraction.put(playerId, playerAttraction);
	}

	public static void updateAttractionsSFP(
			Map<String, Map<Pair<String, String>, Double>> attraction,
			double experience, String playerId, double rho, String reportPrev,
			double rewardPrev, int numMMPrev) {

		Map<Pair<String, String>, Double> playerAttraction = attraction
				.get(playerId);

		for (Pair<String, String> key : playerAttraction.keySet()) {
			if (key.t2.equals(reportPrev)) {
				double newAttr = (rho * experience * playerAttraction.get(key) + rewardPrev)
						/ (rho * experience + 1);
				playerAttraction.put(key, newAttr);
			} else {
				double newAttr = (rho * experience * playerAttraction.get(key) + getExpectedPayoff(
						key.t2, numMMPrev)) / (rho * experience + 1);
				playerAttraction.put(key, newAttr);
			}
		}
		attraction.put(playerId, playerAttraction);
	}

	public static void updateAttractionsEWA(
			Map<String, Map<Pair<String, String>, Double>> attraction,
			double experience, String playerId, double rho, double delta,
			double phi, String signalPrev, String reportPrev,
			double rewardPrev, String signalCurr, int numMMPrev) {

		Map<Pair<String, String>, Double> playerAttraction = attraction
				.get(playerId);

		for (Pair<String, String> key : playerAttraction.keySet()) {
			if (key.t2.equals(reportPrev)) {
				double newAttr = (phi * experience * playerAttraction.get(key) + rewardPrev)
						/ (rho * experience + 1);
				playerAttraction.put(key, newAttr);
			} else {
				double newAttr = (phi * experience * playerAttraction.get(key) + delta
						* getExpectedPayoff(key.t2, numMMPrev))
						/ (rho * experience + 1);
				playerAttraction.put(key, newAttr);
			}
		}
		attraction.put(playerId, playerAttraction);
	}

	static double updateExperience(double experience, double rho) {
		return rho * experience + 1;
	}

	public static Map<String, Double> getStrategy(
			Map<String, Map<Pair<String, String>, Double>> attraction,
			String playerId, boolean considerSignal, double lambda,
			String signalCurrRound, String signalPrevRound) {

		double attrMMReport = 0;
		double attrGBReport = 0;

		Map<Pair<String, String>, Double> playerAttraction = attraction
				.get(playerId);

		if (!considerSignal) {

			attrMMReport = playerAttraction.get(new Pair<String, String>("MM",
					"MM"))
					+ playerAttraction
							.get(new Pair<String, String>("GB", "MM"));
			attrGBReport = playerAttraction.get(new Pair<String, String>("MM",
					"GB"))
					+ playerAttraction
							.get(new Pair<String, String>("GB", "GB"));

		} else if (considerSignal) {

			attrMMReport = playerAttraction.get(new Pair<String, String>(
					signalCurrRound, "MM"));
			attrGBReport = playerAttraction.get(new Pair<String, String>(
					signalCurrRound, "GB"));
		}

		double mmProb = Utils.calcMMProb(lambda, attrMMReport, attrGBReport);
		// Correct for the case when mmProb=1 or mmProb=0
		if (1.0 - mmProb < Utils.eps)
			mmProb = 1.0 - Utils.eps;
		if (mmProb < Utils.eps)
			mmProb = Utils.eps;

		Map<String, Double> strategy = ImmutableMap.of("MM", mmProb, "GB",
				1 - mmProb);
		return strategy;
	}

	private static double getLogLkForReport(Map<String, Double> strategy,
			String reportCurrRound, String playerId) {
		double logLkReport = Math.log(strategy.get(reportCurrRound));
		return logLkReport;
	}

	private static int getNumPossibleRefPlayers(
			Map<String, Map<String, Object>> resultCurrRound,
			String currPlayerId) {
		String refPlayer = (String) resultCurrRound.get(currPlayerId).get(
				"refPlayer");
		String refReport = (String) resultCurrRound.get(refPlayer)
				.get("report");
		int numRefReport = Utils.getNumOfGivenReport(resultCurrRound,
				refReport, currPlayerId);
		return numRefReport;
	}

	public static double getExpectedPayoff(String report, int numOtherMMReports) {

		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			if (numOtherMMReports == 2) {
				return Utils.getPayment(treatment, report, "MM");
			} else if (numOtherMMReports == 0) {
				return Utils.getPayment(treatment, report, "GB");
			} else if (numOtherMMReports == 1) {
				return 0.5 * Utils.getPayment(treatment, report, "MM") + 0.5
						* Utils.getPayment(treatment, report, "GB");
			}

		} else if (treatment.equals("prior2-uniquetruthful")
				|| treatment.equals("prior2-symmlowpay")) {
			return Utils.getPayment(treatment, report, numOtherMMReports);
		}

		return -1;
	}

	public static double[] determinePayoff(String[] reports,
			int[] refPlayerIndices) {

		double[] payoffs = new double[reports.length];

		if (treatment.equals("prior2-basic")
				|| treatment.equals("prior2-outputagreement")) {

			for (int j = 0; j < reports.length; j++) {

				String myReport = reports[j];
				refPlayerIndices[j] = Utils.chooseRefPlayer(j);
				String refReport = reports[refPlayerIndices[j]];
				payoffs[j] = Utils.getPayment(treatment, myReport, refReport);
			}

		} else if (treatment.equals("prior2-uniquetruthful")
				|| treatment.equals("prior2-symmlowpay")) {

			int totalNumMM = 0;
			for (int j = 0; j < reports.length; j++) {
				if (reports[j].equals("MM"))
					totalNumMM++;
			}

			for (int j = 0; j < reports.length; j++) {
				String myReport = reports[j];
				int numOtherMMReports = totalNumMM;
				if (myReport.equals("MM"))
					numOtherMMReports = totalNumMM - 1;
				payoffs[j] = Utils.getPayment(treatment, myReport,
						numOtherMMReports);
			}

		}

		return payoffs;
	}

	/**
	 * Check if players are playing the strategy of always clicking left/right
	 * choice.
	 */
	private static void lazyStrategy() {
		int total = 0;
		int countLeft = 0;
		int numAlwaysLeft = 0;
		int numAlwaysRight = 0;
		for (Game game : expSet.games) {
			for (String hitId : game.playerHitIds) {

				int numLeft = game.getNumLeftChosen(hitId);
				countLeft += numLeft;
				total += game.numRounds;
				if (numLeft == game.numRounds)
					numAlwaysLeft++;
				if (numLeft == 0)
					numAlwaysRight++;
			}
		}
		System.out.println("Number of player always left: " + numAlwaysLeft);
		System.out.println("Number of player always right: " + numAlwaysRight);
		System.out.println("Number of left choices: " + countLeft + ", total: "
				+ total);
		System.out.println("Percent of left choices: " + 100.0 * countLeft
				/ total);
	}

	public static void writeRawDataToFile() throws IOException {
		System.out.println("Write raw data to file");

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

	public static void calcAvgBonus() {
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

	public static void learnHMM() throws IOException {
		System.out.println("Learning HMM");

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

		learntHmm = LogReader
				.learnHMM(expSet.games, numStrategies, numRestarts);
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = LogReader
				.getActObsSequence(expSet.games);
		double loglk = BWToleranceLearner.computeLogLk(learntHmm, seq);

		/*
		 * // list of signal and report observations
		 * List<List<SigActObservation<CandySignal, CandyReport>>> seq =
		 * LogReader.getActObsSequence(expSet.games); BWToleranceLearner bwl =
		 * new BWToleranceLearner(); double loglk = Double.NEGATIVE_INFINITY;
		 * 
		 * // load last best HMM if it exists String filename =
		 * String.format("%slearntHMM%dstrategies.txt", rootDir, numStrategies);
		 * File hmmFile = new File(filename); if (hmmFile.exists()) { learntHmm
		 * = createHMMFromFile(filename); loglk =
		 * BWToleranceLearner.computeLogLk(learntHmm, seq);
		 * System.out.printf("Starting loglikelihood : %.5f\n", loglk); }
		 * 
		 * for (int i = 0; i < numRestarts; i++) {
		 * 
		 * Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp =
		 * Utils.getRandomHmm(numStrategies);
		 * 
		 * Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp =
		 * bwl.learn(origHmmTemp, seq);
		 * 
		 * double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp,
		 * seq);
		 * 
		 * if (loglkTemp > loglk) {
		 * 
		 * learntHmm = learntHmmTemp; loglk = loglkTemp;
		 * 
		 * saveHMMDataToFile(filename, learntHmm); } }
		 */
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

	public static Hmm<SigActObservation<CandySignal, CandyReport>> learnHMM(
			List<Game> games, int numStrategies, int numRestarts)
			throws IOException {

		Hmm<SigActObservation<CandySignal, CandyReport>> bestHMM = null;

		List<List<SigActObservation<CandySignal, CandyReport>>> seq = LogReader
				.getActObsSequence(expSet.games);
		BWToleranceLearner bwl = new BWToleranceLearner();
		double loglk = Double.NEGATIVE_INFINITY;

		String fileame = String.format("%slearntHMM%dstrategies.txt", rootDir,
				numStrategies);

		// load last best HMM if it exists
		File hmmFile = new File(fileame);
		if (hmmFile.exists()) {
			bestHMM = createHMMFromFile(fileame);
			loglk = BWToleranceLearner.computeLogLk(bestHMM, seq);
			// System.out.printf("Starting loglikelihood : %.5f\n", loglk);
		}

		for (int i = 0; i < numRestarts; i++) {

			Hmm<SigActObservation<CandySignal, CandyReport>> origHmmTemp = Utils
					.getRandomHmm(numStrategies);

			Hmm<SigActObservation<CandySignal, CandyReport>> learntHmmTemp = bwl
					.learn(origHmmTemp, seq);

			double loglkTemp = BWToleranceLearner.computeLogLk(learntHmmTemp,
					seq);

			if (loglkTemp > loglk) {
				// save better hmm
				bestHMM = learntHmmTemp;
				loglk = loglkTemp;

				saveHMMDataToFile(fileame, bestHMM);
			}
		}

		return bestHMM;
	}

	public static void setStrategyNames() throws IOException {
		System.out.println("Give strategies names");

		for (int i = 0; i < numStrategies; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = learntHmm
					.getOpdf(i);

			Opdf<SigActObservation<CandySignal, CandyReport>> opdf1 = learntHmm
					.getOpdf(i);

			if (Utils.isMMStrategy(opdf)) {
				if (mmState == -1) {
					strategyNames[i] = "MM";
					mmState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf2 = learntHmm
							.getOpdf(mmState);

					if (Utils.isBetterMMStrategy(opdf1, opdf2)) {

						strategyNames[mmState] = "Mixed";
						mixedState = mmState;

						strategyNames[i] = "MM";
						mmState = i;
					}
				}
			} else if (Utils.isGBStrategy(opdf)) {
				if (gbState == -1) {
					strategyNames[i] = "GB";
					gbState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf4 = learntHmm
							.getOpdf(gbState);

					if (Utils.isBetterGBStrategy(opdf1, opdf4)) {
						strategyNames[gbState] = "Mixed";
						mixedState = gbState;

						strategyNames[i] = "GB";
						gbState = i;
					}
				}
			} else if (Utils.isTruthfulStrategy(opdf)) {
				if (truthfulState == -1) {
					strategyNames[i] = "Truthful";
					truthfulState = i;
				} else {
					Opdf<SigActObservation<CandySignal, CandyReport>> opdf3 = learntHmm
							.getOpdf(truthfulState);

					if (Utils.isBetterTruthfulStrategy(opdf1, opdf3)) {

						strategyNames[truthfulState] = "Mixed";
						mixedState = truthfulState;

						strategyNames[i] = "Truthful";
						truthfulState = i;
					}
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

	public static void genStrategyChangePredictedByHmm() throws IOException {
		System.out.println("Write strategy change predicted by HMM");

		BufferedWriter writer3 = new BufferedWriter(new FileWriter(rootDir
				+ "strategyChangePredictedByHmm.m"));

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

		List<List<SigActObservation<CandySignal, CandyReport>>> seq = LogReader
				.getActObsSequence(expSet.games);
		double loglk;

		BufferedWriter writer = new BufferedWriter(new FileWriter(rootDir
				+ "logLikelihood.m"));

		if (treatment.equals("prior2-basic"))
			writer.write("treatment1loglk = [");
		else if (treatment.equals("prior2-outputagreement"))
			writer.write("treatment2loglk = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writer.write("treatment3loglk = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writer.write("treatment4loglk = [");
		else if (treatment.equals("prior2-constant"))
			writer.write("treatment5loglk = [");

		for (int numStates = 2; numStates <= 6; numStates++) {

			String filename = String.format("%slearntHMM%dstrategies.txt",
					rootDir, numStates);
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(filename);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);

			writer.write(String.format("%.6f ", loglk));
		}
		writer.write("];\n");

		System.out.println("Graph Bayesian information criterion");
		double bic;

		if (treatment.equals("prior2-basic"))
			writer.write("treatment1bic = [");
		else if (treatment.equals("prior2-outputagreement"))
			writer.write("treatment2bic = [");
		else if (treatment.equals("prior2-uniquetruthful"))
			writer.write("treatment3bic = [");
		else if (treatment.equals("prior2-symmlowpay"))
			writer.write("treatment4bic = [");
		else if (treatment.equals("prior2-constant"))
			writer.write("treatment5bic = [");

		for (int numStates = 2; numStates <= 6; numStates++) {

			String filename = String.format("%slearntHMM%dstrategies.txt",
					rootDir, numStates);
			Hmm<SigActObservation<CandySignal, CandyReport>> savedHmm = createHMMFromFile(filename);
			loglk = BWToleranceLearner.computeLogLk(savedHmm, seq);

			int numParams = (numStates * numStates + 2 * numStates - 1);
			int numData = expSet.numGames * expSet.numPlayers
					* expSet.numRounds;
			bic = -2 * loglk + numParams * Math.log(numData);

			writer.write(String.format("%.6f ", bic));
		}
		writer.write("];\n");
		writer.flush();
		writer.close();

	}

	private static List<List<SigActObservation<CandySignal, CandyReport>>> getActObsSequence(
			List<Game> games) {
		List<List<SigActObservation<CandySignal, CandyReport>>> seq = new ArrayList<List<SigActObservation<CandySignal, CandyReport>>>();
		for (Game game : games) {
			for (String hitId : game.playerHitIds) {
				List<SigActObservation<CandySignal, CandyReport>> list = game.signalReportObjList
						.get(hitId);
				seq.add(list);
			}
		}
		return seq;
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

	private static void saveHMMDataToFile(String fileName,
			Hmm<SigActObservation<CandySignal, CandyReport>> hmmToSave)
			throws IOException {

		BufferedWriter writer = new BufferedWriter(new FileWriter(fileName));
		int numStates = hmmToSave.nbStates();
		writer.write(String.format("numStates:%d\n", numStates));

		for (int i = 0; i < numStates; i++) {
			writer.write(String.format("pi,%d:%.17f\n", i, hmmToSave.getPi(i)));
		}

		for (int i = 0; i < numStates; i++) {
			Opdf<SigActObservation<CandySignal, CandyReport>> opdf = hmmToSave
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
						hmmToSave.getAij(i, j)));
			}
		}
		writer.flush();
		writer.close();
	}

	private static Hmm<SigActObservation<CandySignal, CandyReport>> createHMMFromFile(
			String filename) throws IOException {

		BufferedReader reader = new BufferedReader(new FileReader(filename));
		String line = reader.readLine();
		Matcher matcher = Pattern.compile("numStates:(.*)").matcher(line);
		int numStates = -1;
		if (matcher.matches())
			numStates = Integer.parseInt(matcher.group(1));

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
						CandySignal.class, CandyReport.class,
						Utils.signalPrior, probs);
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
		reader.close();
		Hmm<SigActObservation<CandySignal, CandyReport>> hmm = new Hmm<SigActObservation<CandySignal, CandyReport>>(
				pi, aij, opdfs);
		return hmm;
	}

	public static void eqConvergenceSimpleMethod() throws IOException {
		System.out.println("Equilibrium convergence by simple method");

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

	public static void parseLog() {
		System.out.println("Parsing game log");

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

	public static Game parseGameLog(String experimentLogString) {

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

					// System.out.println(currentLine);
					// System.out.println("Real report message found");
					// System.out.printf("%s, %s, %s, %s\n\n",
					// matcherReport.group(1), matcherReport.group(2),
					// matcherReport.group(3), matcherReport.group(4));

					if (treatment.equals("prior2-constant")
							|| treatment.equals("prior2-symmlowpay")) {
						String radio = matcherReport.group(5);
						roundInfo.saveRadio(radio);
					}
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

	public static void writePlayerComments() throws IOException {
		System.out.println("Writer player comments to file");

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

}
