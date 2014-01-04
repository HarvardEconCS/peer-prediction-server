package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.misc.Pair;
import net.andrewmao.models.games.SigActObservation;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

public class Game {

	String id;

	List<Map<String, Double>> worlds;
	double[] priorProbs;

	int numRounds;
	int numPlayers;
	String[] playerHitIds;
	
	double[] paymentArray;
	double[][] paymentArrayComplex;

	Map<String, String> exitSurvey;
	
	List<HITWorker> workers;
	List<Round> rounds;

	Map<String, Double> bonus;
	Map<String, Double> ficPlayPayoff;
	Map<String, Double> bestResponsePayoff;
	
	Map<String, List<String>> reportList;
	Map<String, List<Pair<String, String>>> signalReportPairList;
	Map<String, List<SigActObservation<CandySignal, CandyReport>>> signalReportObjList;
	Map<String, int[]> stateSeq;
		
	Gson gson;
	
	String convergenceType;
	int roundConverged;
	String convergenceTypeRelaxed;
	int roundConvergedRelaxed;

	
	String numberType;
	int number;
	int relaxNum;

	int[] hmmTypeArray;

	public Game() {

		worlds = new ArrayList<Map<String, Double>>();
		rounds = new ArrayList<Round>();
		exitSurvey = new HashMap<String, String>();
		
		bonus = new HashMap<String, Double>();
		ficPlayPayoff = new HashMap<String, Double>();
		bestResponsePayoff = new HashMap<String, Double>();

		gson = new Gson();
		
		convergenceType = "";
		numberType = "";
	}

	public void addRoundInfo(Round roundInfo) {
		rounds.add(roundInfo);
	}

	public void savePriorProb(String probString) {
		priorProbs = gson.fromJson(probString, double[].class);
	}

	public void savePriorWorlds(String worldsString) {
		Object[] worldsArray = gson.fromJson(worldsString, Object[].class);
		for (int i = 0; i < worldsArray.length; i++) {
			Map<String, Double> worldMap = gson.fromJson(
					worldsArray[i].toString(),
					new TypeToken<Map<String, Double>>() {
					}.getType());
			worlds.add(worldMap);
		}
	}

	public void savePlayerHitIds(String playerNamesString) {
		playerHitIds = gson.fromJson(playerNamesString, String[].class);
//		for (String hitId : playerHitIds) {
//			HITWorker worker = new HITWorker();
//			worker.hitId = hitId;
//			workers.add(worker);
//		}
	}

	public void savePaymentRule(String paymentRuleString) {
		if (numPlayers == 3)
			paymentArray = gson.fromJson(paymentRuleString, double[].class);	
	}

	public Map<String, Map<String, Double>> getPlayerStrategyForRoundRange(String hitId,
			int roundNumStart, int roundNumEnd) {
		
		// create and initialize map
		Map<String, Map<String, Double>> strategy = new HashMap<String, Map<String, Double>>();
		for (int i = 0; i < AnalysisUtils.signalList.length; i++) {
			Map<String, Double> value = new HashMap<String, Double>();
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				value.put(AnalysisUtils.signalList[j], 0.0);
			}
			strategy.put(AnalysisUtils.signalList[i], value);
		}

		// fill in map
		for (int i = roundNumStart; i <= roundNumEnd; i++) {
			Round round = rounds.get(i - 1);
			
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			
			double count = strategy.get(signal).get(report);
			count = count + 1;
			strategy.get(signal).put(report, count);
		}

		// round numbers in map to contain 2 decimal places
		for (int i = 0; i < AnalysisUtils.signalList.length; i++) {

			double totalCount = 0;
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				totalCount = totalCount
						+ strategy.get(AnalysisUtils.signalList[i]).get(AnalysisUtils.signalList[j]);
			}
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				double thisCount = strategy
						.get(AnalysisUtils.signalList[i])
						.get(AnalysisUtils.signalList[j]);
				double percent = thisCount / totalCount;
				percent = (double) Math.round(percent * 100) / 100;
				strategy.get(AnalysisUtils.signalList[i])
					.put(AnalysisUtils.signalList[j], percent);
			}
		}

		return strategy;
	}

	public Map<String, Map<String, Double>> getGameStrategyForRoundRange(
			int roundNumStart, int roundNumEnd) {

		// create and initialize map
		Map<String, Map<String, Double>> strategy = new HashMap<String, Map<String, Double>>();
		for (int i = 0; i < AnalysisUtils.signalList.length; i++) {
			Map<String, Double> value = new HashMap<String, Double>();
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				value.put(AnalysisUtils.signalList[j], 0.0);
			}
			strategy.put(AnalysisUtils.signalList[i], value);
		}

		// fill in map
		for (int i = roundNumStart; i <= roundNumEnd; i++) {
			Round round = rounds.get(i - 1);
			for (int j = 0; j < playerHitIds.length; j++) {
				String hitId = playerHitIds[j];
				String signal = round.getSignal(hitId);
				String report = round.getReport(hitId);
				double count = strategy.get(signal).get(report);
				count = count + 1;
				strategy.get(signal).put(report, count);
			}
		}

		// round numbers in map to contain 2 decimal places
		for (int i = 0; i < AnalysisUtils.signalList.length; i++) {

			double totalCount = 0;
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				totalCount = totalCount
						+ strategy.get(AnalysisUtils.signalList[i]).get(AnalysisUtils.signalList[j]);
			}
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				double thisCount = strategy
						.get(AnalysisUtils.signalList[i])
						.get(AnalysisUtils.signalList[j]);
				double percent = thisCount / totalCount;
				percent = (double) Math.round(percent * 100) / 100;
				strategy.get(AnalysisUtils.signalList[i])
					.put(AnalysisUtils.signalList[j], percent);
			}
		}

		return strategy;
	}

	public void printSignalsAndReports() {
		List<List<String>> signals = new ArrayList<List<String>>();
		List<List<String>> reports = new ArrayList<List<String>>();
		int count = 0;
		while (count < numPlayers) {
			List<String> signalList = new ArrayList<String>();
			signals.add(signalList);

			List<String> reportList = new ArrayList<String>();
			reports.add(reportList);

			count++;
		}

		for (Round round : rounds) {
			for (int i = 0; i < playerHitIds.length; i++) {
				signals.get(i).add(round.getSignal(playerHitIds[i]));
				reports.get(i).add(round.getReport(playerHitIds[i]));

			}
		}

		for (int i = 0; i < playerHitIds.length; i++) {
			System.out.printf("%s signals: %s\n", playerHitIds[i],
					signals.get(i).toString());
			System.out.printf("%s reports: %s\n\n", playerHitIds[i], reports
					.get(i).toString());
		}
	}

	public String getStrategyComment(String hitId) {
		String exitSurveyString = exitSurvey.get(hitId);
		Map<String, Object> exitSurveyMap = gson.fromJson(exitSurveyString,
				new TypeToken<Map<String, Object>>() {
				}.getType());
		Object strategyString = exitSurveyMap.get("strategy");
		Map<String, String> strategyMap = (Map<String, String>) strategyString;
		String comments = strategyMap.get("comments").toString();
		return comments;
	}

	public List<Pair<String, String>> getSignalReportPairsForPlayer(String hitId) {
		return signalReportPairList.get(hitId);
//		List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
//		for (Round round : rounds) {
//			String signal = round.getSignal(hitId);
//			String report = round.getReport(hitId);
//			list.add(new Pair<String, String>(signal, report));
//		}
//		return list;
	}

	/**
	 * For learning HMM
	 * @param hitId
	 * @return
	 */
	public List<SigActObservation<CandySignal, CandyReport>> getSignalReportObjForPlayer(String hitId) {
		return signalReportObjList.get(hitId);
//		List<SigActObservation<CandySignal, CandyReport>> list = 
//				new ArrayList<SigActObservation<CandySignal, CandyReport>>();
//		for (Round round : rounds) {
//			String signal = round.getSignal(hitId);
//			String report = round.getReport(hitId);
//			list.add(new SigActObservation<CandySignal, CandyReport>(
//					CandySignal.valueOf(signal), CandyReport.valueOf(report)));
//		}
//		return list;
	}

	public List<Pair<String, String>> getSignalReportPairsForGameRound(
			Game game, Round round) {
		List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
		for (String hitId : game.playerHitIds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			list.add(new Pair<String, String>(signal, report));
		}
		return list;
	}

	
	
	
	public int getCandyStart(String hitId, String candy) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	private int getCandyStartRelaxed(String hitId, String candy, int num) {
		int countRelaxed = 0;
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals(candy))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

	public int getHonestStart(String hitId) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			String signal = rounds.get(i).getSignal(hitId);
			String report = rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else 
				return i + 1;
		}
		return 1;
	}

	private int getHonestStartRelaxed(String hitId, int num) {
		int countRelaxed = 0;
		for (int i = rounds.size() - 1; i >= 0; i--) {
			String signal = rounds.get(i).getSignal(hitId);
			String report = rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else {
				if (countRelaxed < num) {
					countRelaxed++;
				} else {
					return i + 1;
				}
			}
		}
		return 1;
	}

//	public int getGBStart(String hitId) {
//		
//		for (int i = rounds.size() - 1; i >= 0; i--) {
//			if (rounds.get(i).getReport(hitId).equals("GB"))
//				continue;
//			else 
//				return i;
//		}
//		return 0;
//	}

//	private int getGBStartRelaxed(String hitId, int num) {
//		int countRelaxed = 0;
//		for (int i = rounds.size() - 1; i >= 0; i--) {
//			if (rounds.get(i).getReport(hitId).equals("GB"))
//				continue;
//			else {
//				if (countRelaxed < num) {
//					countRelaxed++;
//				} else {
//					return i;
//				}
//			}
//		}
//		return 0;
//	}

	public int getNumMM(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			if (round.getReport(hitId).equals("MM")) count++;
		}
		return count;
	}

	public int getNumGB(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			if (round.getReport(hitId).equals("GB")) count++;
		}
		return count;
	}

	public int getNumHonest(String hitId) {
		int count = 0;
		for (Round round : rounds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			if (signal.equals(report)) count++;
		}
		return count;
	}

	public double getPercentReportGivenSignal(String hitId, String signalAndReport) {
		int countSignal = 0;
		int countReport = 0;
		for (Round round : rounds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			if (signal.equals(signalAndReport)) {
				countSignal++;
				if (report.equals(signalAndReport))
					countReport++;
			}

		}
		return 1.0 * countReport / countSignal;
	}

	public double getPercentReportGivenSignal(String hitId, String signalAndReport,
			int i, int j) {
		int countSignal = 0;
		int countReport = 0;
		for (int k = i; k <= j; k++) {
			String signal = rounds.get(k).getSignal(hitId);
			String report = rounds.get(k).getReport(hitId);
			if (signal.equals(signalAndReport)) {
				countSignal++;
				if (report.equals(signalAndReport))
					countReport++;
			}

		}
		return 1.0 * countReport / countSignal;
	}

	
	public void fillConvergenceType() {
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : this.playerHitIds) {
			
			int playerMMStart = this.getCandyStart(hitId, "MM");
			gameMMStart = Math.max(gameMMStart, playerMMStart);
			
			int playerGBStart = this.getCandyStart(hitId, "GB");
			gameGBStart = Math.max(gameGBStart, playerGBStart);
			
			int playerHOStart = this.getHonestStart(hitId);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		this.roundConverged = min;
		
		String gameType = "";
		if (min > 15) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		this.convergenceType = gameType;
	}
	
	public void fillAsymmetricConvergenceType() {
		// 3 MM, 1 GB
		int best3MMStart = Integer.MAX_VALUE;
		for (int j = 0; j < this.playerHitIds.length; j++) {
			
			int threeMMStart = 0;
			
			for (int k = 0; k < this.playerHitIds.length; k++) {
				if (k == j) {
					int playerGBScore = this.getCandyStart(this.playerHitIds[k], "GB");
					threeMMStart = Math.max(threeMMStart, playerGBScore);
				} else {
					int playerMMScore = this.getCandyStart(this.playerHitIds[k], "MM");
					threeMMStart = Math.max(threeMMStart, playerMMScore);
				}
			}
			
			if (threeMMStart < best3MMStart)
				best3MMStart = threeMMStart;
			
		}
		
		// 3 GB, 1 MM
		int best3GBStart = Integer.MAX_VALUE;
		for (int j = 0; j < this.playerHitIds.length; j++) {
			
			int threeGBStart = 0;
			
			for (int k = 0; k < this.playerHitIds.length; k++) {
				if (k == j) {
					int playerMMScore = this.getCandyStart(this.playerHitIds[k], "MM");
					threeGBStart = Math.max(threeGBStart, playerMMScore);
				} else {
					int playerGBScore = this.getCandyStart(this.playerHitIds[k], "GB");
					threeGBStart = Math.max(threeGBStart, playerGBScore);
				}
			}
			
			if (threeGBStart < best3GBStart)
				best3GBStart = threeGBStart;
			
		}
	
		int gameHOStart = 0; 
		for (String hitId : this.playerHitIds) {
			
			int playerHOStart = this.getHonestStart(hitId);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}
		
		int min = Math.min(Math.min(best3MMStart, best3GBStart), gameHOStart);
		this.roundConverged = min;
		
		String gameType = "";
		if (min > 15) {
			gameType = "undecided";
		} else {
			if (min == best3MMStart) {
				gameType = "3MM";					
			} else if (min == best3GBStart) {
				gameType = "3GB"; 
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		this.convergenceType = gameType;
	}

	public void fillConvergenceTypeRelaxed(int i) {
		String gameType = "";
		
		int gameMMStart = 0;
		int gameGBStart = 0;
		int gameHOStart = 0; 
		
		for (String hitId : playerHitIds) {
			
			int playerMMScore = this.getCandyStartRelaxed(hitId, "MM", i);
			gameMMStart = Math.max(gameMMStart, playerMMScore);
			
			int playerGBScore = this.getCandyStartRelaxed(hitId, "GB", i);
			gameGBStart = Math.max(gameGBStart, playerGBScore);
			
			int playerHOScore = this.getHonestStartRelaxed(hitId, i);
			gameHOStart = Math.max(gameHOStart, playerHOScore);
		}
		
		int min = Math.min(Math.min(gameMMStart, gameGBStart), gameHOStart);
		this.roundConvergedRelaxed = min;
		
		if (min > (15 - i)) {
			gameType = "undecided";
		} else {
			if (gameMMStart == min) {
				gameType = "MM";
			}
			if (gameGBStart == min) {
				gameType = "GB";
			}
			if (gameHOStart == min) {
				gameType = "HO";
			}
		}
		this.convergenceTypeRelaxed = gameType;
	}
	
	public void fillAsymmetricConvergenceTypeRelaxed(int i) {

		// 3 MM, 1 GB
		int bestThreeMMOneGBStart = Integer.MAX_VALUE;
		for (int j = 0; j < playerHitIds.length; j++) {
			
			int threeMMOneGBStart = 0;
			
			for (int k = 0; k < playerHitIds.length; k++) {
				if (k == j) {
					int playerGBScore = this.getCandyStartRelaxed(playerHitIds[k], "GB", i);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerGBScore);
				} else {
					int playerMMScore = this.getCandyStartRelaxed(playerHitIds[k], "MM", i);
					threeMMOneGBStart = Math.max(threeMMOneGBStart, playerMMScore);
				}
			}
			
			if (threeMMOneGBStart < bestThreeMMOneGBStart)
				bestThreeMMOneGBStart = threeMMOneGBStart;
			
		}
		
		// 3 GB, 1 MM
		int bestThreeGBOneMMStart = Integer.MAX_VALUE;
		for (int j = 0; j < playerHitIds.length; j++) {
			
			int threeGBOneMMStart = 0;
			
			for (int k = 0; k < playerHitIds.length; k++) {
				if (k == j) {
					int playerMMScore = this.getCandyStartRelaxed(playerHitIds[k], "MM", i);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerMMScore);
				} else {
					int playerGBScore = this.getCandyStartRelaxed(playerHitIds[k], "GB", i);
					threeGBOneMMStart = Math.max(threeGBOneMMStart, playerGBScore);
				}
			}
			
			if (threeGBOneMMStart < bestThreeGBOneMMStart)
				bestThreeGBOneMMStart = threeGBOneMMStart;
			
		}
		
		int gameHOStart = 0; 
		for (String hitId : this.playerHitIds) {
			
			int playerHOStart = this.getHonestStartRelaxed(hitId, i);
			gameHOStart = Math.max(gameHOStart, playerHOStart);
		}

		int min = Math.min(Math.min(bestThreeMMOneGBStart, bestThreeGBOneMMStart), gameHOStart);
		this.roundConvergedRelaxed = min;

		String gameType = "";
		if (min > (15 - i)) {
			gameType = "undecided";
		} else {
			if (min == bestThreeMMOneGBStart) {
				gameType = "3MM";
			} else if (min == bestThreeGBOneMMStart) {
				gameType = "3GB";
			} else if (min == gameHOStart) {
				gameType = "HO";
			}
		}
		this.convergenceTypeRelaxed = gameType;
	}

	public void populateInfo() {

		reportList = new HashMap<String, List<String>>();
		for (String hitId: playerHitIds) {
			List<String> list = new ArrayList<String>();
			for (Round round : rounds) {
				String report = round.getReport(hitId);
				list.add(report);
			}
			reportList.put(hitId, list);
		}
		
		signalReportPairList = new HashMap<String, List<Pair<String, String>>>();
		for (String hitId : playerHitIds) {
			List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
			for (Round round : rounds) {
				String signal = round.getSignal(hitId);
				String report = round.getReport(hitId);
				list.add(new Pair<String, String>(signal, report));
			}
			signalReportPairList.put(hitId, list);
		}
		
		signalReportObjList = new HashMap<String, List<SigActObservation<CandySignal, CandyReport>>>();
		for (String hitId: playerHitIds) {
			List<SigActObservation<CandySignal, CandyReport>> list = 
					new ArrayList<SigActObservation<CandySignal, CandyReport>>();
			for (Round round : rounds) {
				String signal = round.getSignal(hitId);
				String report = round.getReport(hitId);
				list.add(new SigActObservation<CandySignal, CandyReport>(
					CandySignal.valueOf(signal), CandyReport.valueOf(report)));
			}
			signalReportObjList.put(hitId, list);
		}

	}

	public Map<String, Double> getOppPopStrFull(int round, String excludeHitId) {
		Map<String, Integer> temp = new HashMap<String, Integer>();
		temp.put("MM", 0);
		temp.put("GB", 0);
		for (String playerHitId : playerHitIds) {
			if (playerHitId.equals(excludeHitId))
				continue;
			for (int i = 0; i < rounds.size(); i++) {
				String report = rounds.get(i).getReport(playerHitId);
				int num = temp.get(report);
				num++;
				temp.put(report, num);
			}
		}
		
		Map<String, Double> oppPopStrategy = new HashMap<String, Double>();
		int total = temp.get("MM") + temp.get("GB");
		oppPopStrategy.put("MM", temp.get("MM") * 1.0 / total);
		oppPopStrategy.put("GB", temp.get("GB") * 1.0 / total);
		return oppPopStrategy;
	}

	public Map<String, Double> getOppPopStrPrevRound(int i, String excludeHitId) {
		Map<String, Integer> temp = new HashMap<String, Integer>();
		temp.put("MM", 0);
		temp.put("GB", 0);
		for (String playerHitId : playerHitIds) {
			if (playerHitId.equals(excludeHitId))
				continue;
			String report = rounds.get(i - 1).getReport(playerHitId);
			int num = temp.get(report);
			num++;
			temp.put(report, num);
		}
		
		Map<String, Double> oppPopStrategy = new HashMap<String, Double>();
		int total = temp.get("MM") + temp.get("GB");
		oppPopStrategy.put("MM", temp.get("MM") * 1.0 / total);
		oppPopStrategy.put("GB", temp.get("GB") * 1.0 / total);
		return oppPopStrategy;
	}

	public Map<String, Double> getPlayerHistory(int round, String hitId) {
		Map<String, Integer> temp = new HashMap<String, Integer>();
		temp.put("MM", 0);
		temp.put("GB", 0);		
		for (int i = 0; i < rounds.size(); i++) {
			String report = rounds.get(i).getReport(hitId);
			int num = temp.get(report);
			num++;
			temp.put(report, num);
		}
		Map<String, Double> playerHistory = new HashMap<String, Double>();
		int total = temp.get("MM") + temp.get("GB");
		playerHistory.put("MM", temp.get("MM") * 1.0 / total);
		playerHistory.put("GB", temp.get("GB") * 1.0 / total);
		return playerHistory;
	}

	public double getPayment(String myReport, String refReport) {
		if (myReport.equals("MM") && refReport.equals("MM"))
			return paymentArray[0];
		else if (myReport.equals("MM") && refReport.equals("GB"))
			return paymentArray[1];
		else if (myReport.equals("GB") && refReport.equals("MM"))
			return paymentArray[2];
		return paymentArray[3];
	}

	public double getPaymentComplex(String myReport, int numMMOtherReports) {
		if (myReport.equals("MM")) {
			switch (numMMOtherReports) {
			case 0:
				return 0.9;
			case 1:
				return 0.1;
			case 2:
				return 1.5;
			case 3:
				return 0.8;
			}

		} else {
			switch (numMMOtherReports) {
			case 0:
				return 0.8;
			case 1:
				return 1.5;
			case 2:
				return 0.1;
			case 3:
				return 0.9;
			}
		}
		return -1;
	}

	// public List<String> getSignalsForPlayer(String hitId) {
	// List<String> list = new ArrayList<String>();
	// for (Round round : rounds) {
	// list.add(round.getSignal(hitId));
	// }
	// return list;
	// }

	// public List<String> getReportsForPlayer(String hitId) {
	// List<String> list = new ArrayList<String>();
	// for (Round round : rounds) {
	// list.add(round.getReport(hitId));
	// }
	// return list;
	// }

	// public List<String> getSignalsForRound(int roundNum) {
	// List<String> list = new ArrayList<String>();
	// for (String hitId : playerHitIds) {
	// list.add(rounds.get(roundNum - 1).getSignal(hitId));
	// }
	// return list;
	// }

	// public List<String> getReportsForRound(int roundNum) {
	// List<String> list = new ArrayList<String>();
	// for (String hitId : playerHitIds) {
	// list.add(rounds.get(roundNum - 1).getReport(hitId));
	// }
	// return list;
	// }

}
