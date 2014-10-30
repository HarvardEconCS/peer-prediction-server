package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.cureos.numerics.Calcfc;
import com.cureos.numerics.Cobyla;

public class LearningModelsCustom {

	public static double computeLogLk(String model, Map<String, Object> params,
			List<Game> games) {
		if (model.equals("s1")) {
			return computeLogLkS1(params, games);
		} else if (model.startsWith("s1-1")) {
			return computeLogLkS1Dash1(params, games);
		} else if (model.startsWith("s2")) {
			return computeLogLkS2(params, games);
		} else if (model.startsWith("s3")) {
			return computeLogLkS3(params, games);
		}
		return Double.NEGATIVE_INFINITY;
	}
	
	/**
	 * Model s1:
	 * 
	 * At the beginning of the game, each player draws a strategy from a set of
	 * 5 strategies (truthful, MM, GB, opposite, random) according to a fixed
	 * distribution. Then the player plays this strategy for the entire game.
	 * 
	 * @param params
	 * @param games
	 * @return
	 */
	public static double computeLogLkS1(Map<String, Object> params,
			List<Game> games) {
	 
		double eps = (double) params.get("eps");
		
		double probTR = (double) params.get("probTR");
		double probMM = (double) params.get("probMM");
		double probGB = (double) params.get("probGB");
		double probOP = (double) params.get("probOP");
		double probRA = 1 - probTR - probMM - probGB - probOP;
		if (probTR + probMM + probGB + probOP > 1)
			probRA = 0;

		double loglk = 0;
	
		for (Game game : games) {
	
			for (String playerId : game.playerHitIds) {
	
				double lkPlayer = 
					  probTR * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "TR", eps, null)
					+ probMM * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "MM", eps, null)
					+ probGB * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "GB", eps, null) 
					+ probOP * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "OP", eps, null) 
					+ probRA * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "RA", eps, null);
	
				loglk += Math.log(lkPlayer);
			}
		}
	
		return loglk;
	}

	
	/**
	 * Model s1:
	 * 
	 * At the beginning of the game, each player draws a strategy from a set of
	 * 5 strategies (truthful, MM, GB, opposite, random) according to a fixed
	 * distribution. Then the player plays this strategy for the entire game.
	 * 
	 * @param params
	 * @param games
	 * @return
	 */
	public static double computeLogLkS1Dash1(Map<String, Object> params,
			List<Game> games) {
	 
		double eps = (double) params.get("eps");
		
		double probTR = (double) params.get("probTR");
		double probMM = (double) params.get("probMM");
		double probGB = (double) params.get("probGB");
		double probOP = (double) params.get("probOP");
		double probMixed = 1 - probTR - probMM - probGB - probOP;
		if (probTR + probMM + probGB + probOP> 1)
			probMixed = 0;
		
		double mmGivenMM = (double) params.get("mmGivenMM");
		double mmGivenGB = (double) params.get("mmGivenGB");
		List<Double> strParams = new ArrayList<Double>();
		strParams.add(mmGivenMM);
		strParams.add(mmGivenGB);

		double loglk = 0;
	
		for (Game game : games) {
	
			for (String playerId : game.playerHitIds) {
	
				double lkPlayer = 
					  probTR * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "TR", eps, null)
					+ probMM * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "MM", eps, null)
					+ probGB * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "GB", eps, null) 
					+ probOP * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "OP", eps, null) 
					+ probMixed * helperGetLkStrategy(game, playerId, 0, LogReader.expSet.numRounds, "CU", eps, strParams);
	
				loglk += Math.log(lkPlayer);
			}
		}
	
		return loglk;
	}
	
	/**
	 * Model s2
	 * 
	 * @param params
	 * @param games
	 * @return
	 */
	public static double computeLogLkS2(Map<String, Object> params,
			List<Game> games) {
		return 0;
	}

	/**
	 * Model s3:
	 * 
	 * At the beginning, each player draws a strategy from a set of five
	 * strategies (truthful, MM, GB, opposite, random) according to a fixed
	 * distribution. At every round, the player compares his actual payoff to
	 * his hypothetical payoff if he followed one of the five given strategies
	 * from the beginning. If his actual payoff is worse than the best
	 * alternative payoff by an additive/multiplicative factor of delta, then
	 * the player switches to the best alternative strategy and plays it until
	 * the end of the game. Otherwise, the player plays his original strategy
	 * until the end of the game.
	 * 
	 * @param params
	 * @param games
	 * @return
	 */
	public static double computeLogLkS3(Map<String, Object> params,	List<Game> games) {
	
		boolean isAbs = (boolean) params.get("isAbs");
		
		double eps = (double) params.get("eps");
		double delta = (double) params.get("delta");
		
		double probTR = (double) params.get("probTR");
		double probMM = (double) params.get("probMM");
		double probGB = (double) params.get("probGB");
		double probOP = (double) params.get("probOP");
		double probRA = 1 - probTR - probMM - probGB - probOP;
		if (probTR + probMM + probGB + probOP > 1)
			probRA = 0;

		double loglk = 0;
	
		for (Game game : games) {
	
			for (String playerId : game.playerHitIds) {
	
				// get round switched and new strategy index
				int[] switchInfo = getSwitchInfoS3(game, playerId, isAbs, delta);
				int roundSwitched = switchInfo[0];
	
				// likelihood before switching
				double lkBeforeSwitch = 
						  probTR * helperGetLkStrategy(game, playerId, 0, roundSwitched, "TR", eps, null) 
						+ probMM * helperGetLkStrategy(game, playerId, 0, roundSwitched, "MM", eps, null)
						+ probGB * helperGetLkStrategy(game, playerId, 0, roundSwitched, "GB", eps, null)
						+ probOP * helperGetLkStrategy(game, playerId, 0, roundSwitched, "OP", eps, null)
						+ probRA * helperGetLkStrategy(game, playerId, 0, roundSwitched, "RA", eps, null);  
				
				// likelihood after switching
				int indexNewStrategy = switchInfo[1];
				String newStrategy = strategyIndexToStringS3(indexNewStrategy);
				double lkAfterSwitch = helperGetLkStrategy(game, playerId,
						roundSwitched, LogReader.expSet.numRounds, newStrategy, eps, null);
	
				// add player log likelihood to total log likelihood
				loglk += Math.log(lkBeforeSwitch) + Math.log(lkAfterSwitch);
			}
		}
	
		return loglk;
	}

	static int[] getSwitchInfoS3(Game game, String playerId, boolean isAbs, double delta) {
		double actualPayoff = 0.0; 
		int numStrategies = 5;
		List<Double> hypoPayoffs = initHypoPayoffsS3(numStrategies);
		
		int round;
		int indexStrategy = -1;
		for (round = 0; round < LogReader.expSet.numRounds; round++) {
			
			Double bestAltPayoff = Collections.max(hypoPayoffs);
			if ((isAbs && shouldSwitchAbsS3(bestAltPayoff, actualPayoff, delta)) 
					|| (!isAbs && shouldSwitchRelS3(bestAltPayoff, actualPayoff, delta)) ) {
				
				indexStrategy = hypoPayoffs.indexOf(bestAltPayoff);
				break;
			}
			
			Round r = game.rounds.get(round);
			String signal = r.getSignal(playerId);
			double reward = r.getReward(playerId);
			
			// update actual and hypothetical payoffs
			actualPayoff += reward;
			updateHypoPayoffsS3(hypoPayoffs, playerId, signal, r, LogReader.treatment);

		}
		return new int[] { round, indexStrategy };
	}

	static boolean shouldSwitchAbsS3(double bestPayoff, double actualPayoff, double delta) {
		return bestPayoff > actualPayoff + delta;
	}

	static boolean shouldSwitchRelS3(double bestPayoff, double actualPayoff, double delta) {
		return bestPayoff > actualPayoff * delta;
	}

	static List<Double> initHypoPayoffsS3(int numStrategies) {
		List<Double> hypoPayoffs = new ArrayList<Double>();		
		for (int i = 0; i < numStrategies; i++) {
			hypoPayoffs.add(new Double(0.0));
		}
		return hypoPayoffs;
	}

	/**
	 * 0: TR, 1: MM, 2: GB, 3: OP, 4: RA
	 */
	static void updateHypoPayoffsS3(List<Double> hypoPayoffs, String playerId,
			String signal, Round r, String treatment) {
		
		int index = 0;
		double payoffTruthful = hypoPayoffs.get(index)
				+ r.getHypoReward(treatment, playerId, signal);
		hypoPayoffs.set(index, payoffTruthful);

		index = 1;
		double payoffMM = hypoPayoffs.get(index)
				+ r.getHypoReward(treatment, playerId, "MM");
		hypoPayoffs.set(index, payoffMM);

		index = 2;
		double payoffGB = hypoPayoffs.get(index)
				+ r.getHypoReward(treatment, playerId, "GB");
		hypoPayoffs.set(index, payoffGB);
		
		index = 3;
		double payoffOP = hypoPayoffs.get(index)
				+ r.getHypoReward(treatment, playerId, Utils.getOtherReport(signal));
		hypoPayoffs.set(index, payoffOP);

		index = 4;
		double payoffRA = hypoPayoffs.get(index) 
				+ 0.5 * r.getHypoReward(treatment, playerId, "MM") 
				+ 0.5 * r.getHypoReward(treatment, playerId, "GB");
		hypoPayoffs.set(index, payoffRA);
	}
	
	/**
	 * Convert strategy index to string
	 * @param strategyIndex
	 * @return
	 */
	static String strategyIndexToStringS3(int strategyIndex) {
		String strategyName = "";
		switch (strategyIndex) {
		case 0:
			strategyName = "TR";
			break;
		case 1: 
			strategyName = "MM";
			break;
		case 2:
			strategyName = "GB";
			break;
		case 3:
			strategyName = "OP";
			break;
		case 4:
			strategyName = "RA";
			break;
		}
		return strategyName;
	}

	/**
	 * @param game
	 * @param playerId
	 * @param roundStart inclusive
	 * @param roundEnd exclusive
	 * @param strategy
	 *            TR for truthful strategy, MM or GB for the constant reporting
	 *            strategy, OP for the always reporting opposite strategy
	 * @param eps
	 * @return
	 */
	static double helperGetLkStrategy(Game game, String playerId, int roundStart,
			int roundEnd, String strategy, double eps, List<Double> strParams) {
		double lk = 1.0;
		
		if (strategy.equals("RA"))
			return Math.pow(0.5, roundEnd - roundStart);
	
		for (int i = roundStart; i < roundEnd; i++) {
			String signal = game.rounds.get(i).getSignal(playerId);
			String report = game.rounds.get(i).getReport(playerId);
	
			switch (strategy) {
			case "TR":
				if (signal.equals(report)) lk *= 1 - eps;
				else lk *= eps;
				break;
			case "TRFixed":
				if (signal.equals("MM")) {
					if (report.equals("MM"))
						lk *= 0.99;
					else 
						lk *= 1 - 0.99;
				} else if (signal.equals("GB")) {
					if (report.equals("MM"))
						lk *= 0.04;
					else 
						lk *= 1 - 0.04;
				}
				break;
			case "MM":
				if (report.equals(strategy)) lk *= 1 - eps;
				else lk *= eps;
				break;
			case "MMFixed":
				if (signal.equals("MM")) {
					if (report.equals("MM"))
						lk *= 0.98;
					else 
						lk *= 1 - 0.98;
				} else if (signal.equals("GB")) {
					if (report.equals("MM"))
						lk *= 0.9;
					else 
						lk *= 1 - 0.9;
				}
				break;
			case "GB":
				if (report.equals(strategy)) lk *= 1 - eps;
				else lk *= eps;
				break;
			case "GBFixed":
				if (signal.equals("MM")) {
					if (report.equals("MM"))
						lk *= 0.13;
					else 
						lk *= 1 - 0.13;
				} else if (signal.equals("GB")) {
					if (report.equals("MM"))
						lk *= 0.09;
					else 
						lk *= 1 - 0.09;
				}
				break;
			case "OP":
				if (!signal.equals(report))	lk *= 1 - eps;
				else lk *= eps;
				break;
			case "CU":
				if (signal.equals("MM")) {
					if (report.equals("MM"))
						lk *= strParams.get(0);
					else 
						lk *= 1 - strParams.get(0);
				} else if (signal.equals("GB")) {
					if (report.equals("MM"))
						lk *= strParams.get(1);
					else 
						lk *= 1 - strParams.get(1);
				}
				break;
			default:
				System.out.println("Unrecognized strategy");
				return -1.0;
			}
	
		}
		return lk;
	}

	public static double[] estimateUsingCobyla(String model,
			List<Game> trainingSet) {

		double rhobeg = 0.5;
		double rhoend = 1e-10;
		int iprint = 0;
		int maxfun = 10000;

		// set parameters
		int[] cobylaParams = setCobylaParams(model);
		int numVariables = cobylaParams[0];
		int numConstraints = cobylaParams[1];

		// objective function
		Calcfc function = new LogLkFunctionCobyla(trainingSet, model);

		int restartIndex = 0;
		int numRestarts = 10;
		double[] point = null;
		boolean shouldStop = false;

		double bestLogLk = Double.NEGATIVE_INFINITY;
		double[] bestPoint = null;

		while (!shouldStop) {
			point = setRandomStartPoint(model);
			if (model.equals("s1-1")) {
				point[4] = 0.5 / numRestarts * restartIndex;
			}
			
//			if (model.startsWith("s3")) {
//				point[4] = getUBCobyla(model, "delta") / numRestarts
//						* restartIndex;
//			}
			
			Cobyla.FindMinimum(function, numVariables, numConstraints, point,
					rhobeg, rhoend, iprint, maxfun);

			// if constraints are violated
			if (LearningModelsCustom.constraintsViolated(model, point)) {
				((LogLkFunctionCobyla) function).squarePenCoeff();
				continue;
			}
			
			// if eps > 0.1
//			if (model.equals("s1-1") && point[4] > 0.1) {
//				System.out.println("eps > 0.1, "
//						+ pointToMap(model, point).toString());
//				continue;
//			}
			
			double loglk = computeLogLk(model, pointToMap(model, point), trainingSet);
			if (loglk > bestLogLk) {
//				System.out.println(pointToMap(model, point));
				bestLogLk = loglk;
				bestPoint = point;
			}

			restartIndex++;
			if (restartIndex == numRestarts)
				shouldStop = true;

		}
		return bestPoint;
	}
	
	static int[] setCobylaParams(String model) {
		
		int numVariables = 0;
		int numConstraints = 0;
		
		if (model.startsWith("s3")) {
			numVariables = 6;
			numConstraints = 9;
		} else if (model.equals("s2")) {
			numVariables = 6;
			numConstraints = 9;
		} else if (model.equals("s1-1")) {
			numVariables = 7;
			numConstraints = 11;
		} else if (model.equals("s1")) {
			numVariables = 5;
			numConstraints = 7;

		} else if (model.startsWith("SFP") || model.startsWith("RL")) {
			numVariables = 2;
			numConstraints = 4;

		}
		return new int[] { numVariables, numConstraints };
	}

	/**
	 * Get random starting point
	 * @param model
	 * @return
	 */
	static double[] setRandomStartPoint(String model) {
		
		double[] randomVec5 = Utils.getRandomVec(5);
		
		if (model.startsWith("s3")) {
			
			double epsStart = Utils.rand.nextDouble() * getUBCobyla(model, "eps");
			double deltaStart = Utils.rand.nextDouble() * getUBCobyla(model, "delta");
//			double deltaStart = 5;
			
			return new double[] { randomVec5[0], randomVec5[1], randomVec5[2], randomVec5[3],
					epsStart, deltaStart };
			
			
		} else if (model.equals("s1-1")) {
			
			double epsStart = Utils.rand.nextDouble() * getUBCobyla(model, "eps");
			double mmGivenMM = Utils.rand.nextDouble();
			double mmGivenGB = Utils.rand.nextDouble();
			
			return new double[] { randomVec5[0], randomVec5[1], randomVec5[2], randomVec5[3],
					epsStart, mmGivenMM, mmGivenGB};
	
			
		} else if (model.equals("s1")) {
			
			double epsStart = Utils.rand.nextDouble() * getUBCobyla(model, "eps");
			
			return new double[] { randomVec5[0], randomVec5[1], randomVec5[2], randomVec5[3], 
					epsStart};
	
			
		} else if (model.equals("s2")) {
			// TODO

		} else if (model.startsWith("RL") || model.startsWith("SFP")) {
			return new double[] { 0.0, 1.0 };
		}
		return null;
	}

	static boolean constraintsViolated(String model, double[] point) {

		if (model.startsWith("s3")) {

			if (point[0] < 0 || point[1] < 0 || point[2] < 0 || point[3] < 0) {
				return true;
			}
			if (point[0] + point[1] + point[2] + point[3] > 1) {
				return true;
			}
			
			double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
			if (point[4] < 0 || point[4] > epsUB) {
				return true;
			}
			
			double deltaLB = LearningModelsCustom.getLBCobyla(model, "delta");
			double deltaUB = LearningModelsCustom.getUBCobyla(model, "delta");
			if (point[5] < deltaLB || point[5] > deltaUB) {
				return true;
			}
			
		} else if (model.equals("s2")) {
			// TODO
		} else if (model.equals("s1")) {
			
			if (point[0] < 0 || point[1] < 0 || point[2] < 0 || point[3] < 0)
				return true;
			if (point[0] + point[1] + point[2] + point[3] > 1)
				return true;
			
			double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
			if (point[4] < 0 || point[4] > epsUB)
				return true;

		} else if (model.equals("s1-1")) {
			
			if (point[0] < 0 || point[1] < 0 || point[2] < 0 || point[3] < 0)
				return true;
			if (point[0] + point[1] + point[2] + point[3] > 1)
				return true;
			
			double epsUB = LearningModelsCustom.getUBCobyla(model, "eps");
			if (point[4] < 0 || point[4] > epsUB)
				return true;
			
			if (point[5] < 0 || point[5] > 1) 
				return true;
			if (point[6] < 0 || point[6] > 1)
				return true;

		}
		return false;
	}

	/**
	 * Get upper bounds
	 * @param model
	 * @param paramName
	 * @return
	 */
	public static double getUBCobyla(String model, String paramName) {
		if (paramName.equals("eps"))
			return 0.5;

		if (model.startsWith("s2")) {
			
			if (paramName.equals("delta")) {
				return (1.5 - 0.1) * LogReader.expSet.numRounds;
			}
			
		} else if (model.startsWith("s3")) {

			if (paramName.equals("delta")) {
				if (model.endsWith("abs")) {
					return (1.5 - 0.1) * LogReader.expSet.numRounds;
				} else if (model.endsWith("rel")) {
					return (1.5 / 0.1) * LogReader.expSet.numRounds;
				}
			}

		} else if (model.equals("s1")) {
		}
		return Double.POSITIVE_INFINITY;
	}
	
	/**
	 * get lower bounds
	 * @param model
	 * @param paramName
	 * @return
	 */
	public static double getLBCobyla(String model, String paramName) {
		if (paramName.equals("eps"))
			return 0.0;
		
		if (model.startsWith("s3")) {
			
			if (paramName.equals("delta")) {
				if (model.endsWith("abs")) {
					return 0.0;
				} else if (model.endsWith("rel")) {
					return 1.0;
				}
			}
			
		}
		return Double.NEGATIVE_INFINITY;
	}

	/**
	 * Convert parameter point to map
	 * @param model
	 * @param point
	 * @return
	 */
	static Map<String, Object> pointToMap(String model, double[] point) {
		Map<String, Object> params = new HashMap<String, Object>();
		
		if (model.equals("s1")) {
			
			params.put("probTR", point[0]);
			params.put("probMM", point[1]);
			params.put("probGB", point[2]);
			params.put("probOP", point[3]);
			params.put("eps", 	 point[4]);
			
		} else if (model.equals("s1-1")) {

			params.put("probTR", 	point[0]);
			params.put("probMM", 	point[1]);
			params.put("probGB", 	point[2]);
			params.put("probOP", 	point[3]);
			params.put("eps", 	 	point[4]);
			params.put("mmGivenMM", point[5]);
			params.put("mmGivenGB", point[6]);
			
		} else if (model.startsWith("s3")) {
			
			boolean isAbs = model.split("-")[1].equals("abs");
			params.put("isAbs", isAbs);
			
			params.put("probTR", point[0]);
			params.put("probMM", point[1]);
			params.put("probGB", point[2]);
			params.put("probOP", point[3]);
			params.put("eps", 	 point[4]);
			params.put("delta",  point[5]);
			
		}
		return params;
	}

	public static void addToParamRoundSwitched(Map<String, Object> params, String model) {
		int roundSwitched = Integer.parseInt(model.split("-")[1]);
		params.put("roundSwitched", roundSwitched);
	}

}
