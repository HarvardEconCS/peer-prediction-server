package edu.harvard.econcs.peerprediction.analysis;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.misc.Pair;

import org.apache.commons.math3.exception.MathIllegalStateException;
import org.apache.commons.math3.optim.InitialGuess;
import org.apache.commons.math3.optim.PointValuePair;
import org.apache.commons.math3.optim.SimpleBounds;
import org.apache.commons.math3.optim.SimpleValueChecker;
import org.apache.commons.math3.optim.nonlinear.scalar.GoalType;
import org.apache.commons.math3.optim.nonlinear.scalar.ObjectiveFunction;
import org.apache.commons.math3.optim.nonlinear.scalar.noderiv.CMAESOptimizer;
import org.apache.commons.math3.random.JDKRandomGenerator;

import com.google.common.collect.ImmutableMap;

public class LearningModelsExisting {

	/**
	 * Reinforcement learning
	 */
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
			Map<String, Map<Pair<String, String>, Double>> attraction = LearningModelsExisting.initAttraction(game.playerHitIds);
	
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;
	
				// if first two treatments, add LogLk for reward except last
				// round
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					if (i != LogReader.expSet.numRounds - 1) {
	
						for (String currPlayerId : game.playerHitIds) {
	
							int numRefReport = LearningModelsExisting.getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numRefReport * 1.0
									/ (LogReader.expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}
	
					}
	
				}
	
				// add LogLk for report
				if (i == 0) {
	
					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							LogReader.expSet.numPlayers));
	
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
						LearningModelsExisting.updateAttractionsRL(attraction, playerId, phi,
								signalPrevRound, reportPrevRound,
								rewardPrevRound);
	
						// determine strategy
						Map<String, Double> strategy = LearningModelsExisting.getStrategy(attraction,
								playerId, considerSignal, lambda,
								signalCurrRound, signalPrevRound);
	
						// get loglk for report
						logLkGame += LearningModelsExisting.getLogLkForReport(strategy,
								reportCurrRound, playerId);
	
					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	/**
	 * Stochastic fictitious play
	 */
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
			Map<String, Map<Pair<String, String>, Double>> attractions = LearningModelsExisting.initAttraction(game.playerHitIds);
	
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					// add LogLk for reward except last round
					if (i != LogReader.expSet.numRounds - 1) {
	
						for (String currPlayerId : game.playerHitIds) {
	
							/**
							 * Given the reward of the current player, get
							 * number of other players that could possibly have
							 * been the reference player.
							 */
							int numPossibleRefPlayers = LearningModelsExisting.getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numPossibleRefPlayers
									* 1.0 / (LogReader.expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}
	
					}
	
				}
	
				if (i == 0) {
	
					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							LogReader.expSet.numPlayers));
	
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
						LearningModelsExisting.updateAttractionsSFP(attractions, experiences,
								playerId, rho, reportPrev, rewardPrev,
								numOtherMMReportsPrev);
	
						// update experiences
						experiences = LearningModelsExisting.updateExperience(experiences, rho);
	
						// get strategy
						Map<String, Double> strategy = LearningModelsExisting.getStrategy(attractions,
								playerId, considerSignal, lambda, signalCurr,
								signalPrev);
	
						// add loglk for report
						logLkGame += LearningModelsExisting.getLogLkForReport(strategy, reportCurr,
								playerId);
	
					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	/**
	 * Experience weighted attraction
	 */
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
			Map<String, Map<Pair<String, String>, Double>> attractions = LearningModelsExisting.initAttraction(game.playerHitIds);
	
			for (int i = 0; i < LogReader.expSet.numRounds; i++) {
	
				Map<String, Map<String, Object>> resultCurrRound = game.rounds
						.get(i).result;
	
				if (LogReader.treatment.equals("prior2-basic")
						|| LogReader.treatment.equals("prior2-outputagreement")) {
	
					// add LogLk for reward except last round
					if (i != LogReader.expSet.numRounds - 1) {
	
						for (String currPlayerId : game.playerHitIds) {
	
							/**
							 * Given the reward of the current player, get
							 * number of other players that could possibly have
							 * been the reference player.
							 */
							int numPossibleRefPlayers = LearningModelsExisting.getNumPossibleRefPlayers(
									resultCurrRound, currPlayerId);
							double logLkReward = Math.log(numPossibleRefPlayers
									* 1.0 / (LogReader.expSet.numPlayers - 1));
							logLkGame += logLkReward;
						}
	
					}
	
				}
	
				// add LogLk for report
				if (i == 0) {
	
					logLkGame += Math.log(Math.pow(firstRoundMMProb,
							LogReader.expSet.numPlayers));
	
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
						LearningModelsExisting.updateAttractionsEWA(attractions, experience, playerId,
								rho, delta, phi, signalPrevRound,
								reportPrevRound, rewardPrevRound,
								reportCurrRound, numOtherMMReportsPrev);
	
						// update experiences
						experience = LearningModelsExisting.updateExperience(experience, rho);
	
						// get strategy
						Map<String, Double> strategy = LearningModelsExisting.getStrategy(attractions,
								playerId, considerSignal, lambda,
								signalCurrRound, signalPrevRound);
	
						// add loglk for report
						logLkGame += LearningModelsExisting.getLogLkForReport(strategy,
								reportCurrRound, playerId);
					}
				}
			}
			loglk += logLkGame;
		}
		return loglk;
	}

	public static Map<String, Map<Pair<String, String>, Double>> initAttraction(
			String[] playerHitIds) {
		Map<String, Map<Pair<String, String>, Double>> attraction = new HashMap<String, Map<Pair<String, String>, Double>>();
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
				double newAttr = (rho * experience * playerAttraction.get(key) + LearningModelsExisting.getExpectedPayoff(
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
						* LearningModelsExisting.getExpectedPayoff(key.t2, numMMPrev))
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

	static double getLogLkForReport(Map<String, Double> strategy,
			String reportCurrRound, String playerId) {
		double logLkReport = Math.log(strategy.get(reportCurrRound));
		return logLkReport;
	}

	static int getNumPossibleRefPlayers(
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
	
		if (LogReader.treatment.equals("prior2-basic")
				|| LogReader.treatment.equals("prior2-outputagreement")) {
	
			if (numOtherMMReports == 2) {
				return Utils.getPayment(LogReader.treatment, report, "MM");
			} else if (numOtherMMReports == 0) {
				return Utils.getPayment(LogReader.treatment, report, "GB");
			} else if (numOtherMMReports == 1) {
				return 0.5 * Utils.getPayment(LogReader.treatment, report, "MM") + 0.5
						* Utils.getPayment(LogReader.treatment, report, "GB");
			}
	
		} else if (LogReader.treatment.equals("prior2-uniquetruthful")
				|| LogReader.treatment.equals("prior2-symmlowpay")) {
			return Utils.getPayment(LogReader.treatment, report, numOtherMMReports);
		}
	
		return -1;
	}

	static Map<String, Object> getBounds(String model) {
		Map<String, Object> bounds = new HashMap<String, Object>();
		if (model.equals("s1")) {
			bounds.put("lb", new double[] { 0, 0, 0, 0 });
			bounds.put("ub", new double[] { 1, 1, 1, 0.5 });
			bounds.put("sigma", new double[] { 0.5, 0.5, 0.5, 0.1 });
	
		} else if (model.startsWith("s3")) {
			bounds.put("lb", new double[] { 0, 0, 0, 0, 0 });
			bounds.put("ub", new double[] { 1, 1, 1, 0.5, 1 });
	
		} else if (model.startsWith("RL") || model.startsWith("SFP")) {
			bounds.put("lb", new double[] { 0, 1 });
			bounds.put("ub", new double[] { 1, 10 });
	
		} else if (model.startsWith("EWA")) {
			bounds.put("lb", new double[] { 0, 0, 0, 1 });
			bounds.put("ub", new double[] { 1, 1, 1, 10 });
	
		}
		return bounds;
	}

	static double[] estimateUsingApacheOptimizer(List<Game> games, String model) {
	
		// objective function
		LogLkFunctionApache function = new LogLkFunctionApache(games, model);
	
		// simple upper and lower bounds
		Map<String, Object> bounds = getBounds(model);
		double[] lb = (double[]) bounds.get("lb");
		double[] ub = (double[]) bounds.get("ub");
		double[] sigma = (double[]) bounds.get("sigma");
	
		// optimizer
		// BOBYQAOptimizer optimizer = new BOBYQAOptimizer(lb.length + 2, 10,
		// 1e-12);
		JDKRandomGenerator random = new JDKRandomGenerator();
		random.setSeed(1503);
		SimpleValueChecker checker = new SimpleValueChecker(1E-3, 1E-6);
		CMAESOptimizer cmaesOptimizer = new CMAESOptimizer(Integer.MAX_VALUE,
				1e-10, true, 1, 1, random, false, checker);
	
		// starting point
		double[] startPoint = LearningModelsCustom.setRandomStartPoint(model);
	
		double[] point = startPoint;
		boolean shouldStop = false;
		while (!shouldStop) {
	
			PointValuePair optimum = null;
			try {
				optimum = cmaesOptimizer.optimize(new ObjectiveFunction(
						function), GoalType.MAXIMIZE, new CMAESOptimizer.Sigma(
						sigma), new CMAESOptimizer.PopulationSize(25),
						new InitialGuess(startPoint), new SimpleBounds(lb, ub));
	
				// optimum = optimizer.optimize(new MaxEval(10000000),
				// new ObjectiveFunction(function), GoalType.MAXIMIZE,
				// new InitialGuess(startPoint), new SimpleBounds(lb, ub));
			} catch (MathIllegalStateException e) {
				e.printStackTrace();
				System.exit(0);
				shouldStop = false;
				startPoint = LearningModelsCustom.setRandomStartPoint(model);
				continue;
			}
	
			point = optimum.getPoint();
			// System.out.println(Arrays.toString(point));
	
			if (!LearningModelsCustom.constraintsViolated(model, point)) {
				System.out.println("constraints violated");
				shouldStop = true;
			} else {
				function.squarePenCoeff();
				startPoint = LearningModelsCustom.setRandomStartPoint(model);
			}
		}
	
		return point;
	}

}
