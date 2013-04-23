package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.misc.Pair;

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
//	String[] signalList;

	List<HITWorker> workers;
	
	List<Round> rounds;

	Map<String, String> exitSurvey;

	String convergenceType;

	int roundConverged;

	Gson gson;

	String numberType;

	int number;

	public Game() {
		worlds = new ArrayList<Map<String, Double>>();
		rounds = new ArrayList<Round>();
		exitSurvey = new HashMap<String, String>();
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
		paymentArray = gson.fromJson(paymentRuleString, double[].class);
	}

	public Map<String, Map<String, Double>> getStrategyForRoundRange(
			int roundNumStart, int roundNumEnd) {

		Map<String, Map<String, Double>> strategy = new HashMap<String, Map<String, Double>>();
		for (int i = 0; i < AnalysisUtils.signalList.length; i++) {
			Map<String, Double> value = new HashMap<String, Double>();
			for (int j = 0; j < AnalysisUtils.signalList.length; j++) {
				value.put(AnalysisUtils.signalList[j], 0.0);
			}
			strategy.put(AnalysisUtils.signalList[i], value);
		}

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
		List<Pair<String, String>> list = new ArrayList<Pair<String, String>>();
		for (Round round : rounds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			list.add(new Pair<String, String>(signal, report));
		}
		return list;
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

	public int getMMStart(String hitId) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals("MM"))
				continue;
			else 
				return i;
		}
		return 0;
	}

	public int getHonestStart(String hitId) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			String signal = rounds.get(i).getSignal(hitId);
			String report = rounds.get(i).getReport(hitId);
			if (signal.equals(report))
				continue;
			else 
				return i;
		}
		return 0;
	}

	public int getGBStart(String hitId) {
		for (int i = rounds.size() - 1; i >= 0; i--) {
			if (rounds.get(i).getReport(hitId).equals("GB"))
				continue;
			else 
				return i;
		}
		return 0;
	}

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
