package edu.harvard.econcs.peerprediction.analysis;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import net.andrewmao.models.games.SigActObservation;

public class Game {

	String id;

//	List<Map<String, Double>> worlds;
//	List<Double> priorProbs;

	String[] playerHitIds;
	List<Round> rounds;
	Map<String, Double> bonus;
	
//	double[] paymentArrayT12;

	Map<String, ExitSurvey> exitSurvey;
	
	// For old eq analysis
	String convergenceType = "";
	int roundConverged;
	String convergenceTypeRelaxed;
	int roundConvergedRelaxed;

	// For HMM analysis
	int[] strategyComboTypeArray;
	Map<String, int[]> stateSeq;

	public Game() {
//		worlds = new ArrayList<Map<String, Double>>();
		rounds = new ArrayList<Round>();
		bonus = new HashMap<String, Double>();
		exitSurvey = new HashMap<String, ExitSurvey>();
	}

	public void savePlayerHitIds(String playerNamesString) {
		playerHitIds = Utils.gson.fromJson(playerNamesString, String[].class);
	}
	
	public List<SigActObservation<CandySignal, CandyReport>> getSignalReportPairList(
			String hitId) {

		List<SigActObservation<CandySignal, CandyReport>> list = 
				new ArrayList<SigActObservation<CandySignal, CandyReport>>();
		for (Round round : rounds) {
			String signal = round.getSignal(hitId);
			String report = round.getReport(hitId);
			list.add(new SigActObservation<CandySignal, CandyReport>(
					CandySignal.valueOf(signal), CandyReport.valueOf(report)));
		}

		return list;
	}

//	public void savePriorProb(String probString) {
//	double[] priorProbArray = Utils.gson.fromJson(probString, double[].class);
//	List<Double> priorProbList = new ArrayList<Double>();
//	for (double prob : priorProbArray) {
//		priorProbList.add(prob);
//	}
//	priorProbs = priorProbList;
//}

//public void savePriorWorlds(String worldsString) {
//	Object[] worldsArray = Utils.gson.fromJson(worldsString, Object[].class);
//	for (int i = 0; i < worldsArray.length; i++) {
//		Map<String, Double> worldMap = Utils.gson.fromJson(
//				worldsArray[i].toString(),
//				new TypeToken<Map<String, Double>>() {
//				}.getType());
//		worlds.add(worldMap);
//	}
//}
	
//	public void savePaymentRule(String paymentRuleString) {
////		paymentArrayT12 = gson.fromJson(paymentRuleString, double[].class);	
//	}

//	public int getNumMM(String hitId) {
//		int count = 0;
//		for (Round round : rounds) {
//			if (round.getReport(hitId).equals("MM")) count++;
//		}
//		return count;
//	}
//
//	public int getNumGB(String hitId) {
//		int count = 0;
//		for (Round round : rounds) {
//			if (round.getReport(hitId).equals("GB")) count++;
//		}
//		return count;
//	}
//
//	public int getNumHonest(String hitId) {
//		int count = 0;
//		for (Round round : rounds) {
//			String signal = round.getSignal(hitId);
//			String report = round.getReport(hitId);
//			if (signal.equals(report)) count++;
//		}
//		return count;
//	}
	
//	public Map<String, Double> getOppPopStrFull(int i, String excludeHitId) {
//		
//		List<String> otherReports = new ArrayList<String>();
//		for (Round round : this.rounds) {
//			List<String> reports = this.getOtherReportList(round, excludeHitId);
//			otherReports.addAll(reports);
//		}
//		
//		return Utils.getOppPopStr(otherReports);
//	}

//	public Map<String, Double> getOppPopStrPrevRound(int i, String excludeHitId) {
//		
//		Round prevRound = rounds.get(i - 1);
//		List<String> otherReports = this.getOtherReportList(prevRound, excludeHitId);
//		
//		return Utils.getOppPopStr(otherReports);
//	}

//	public List<String> getOtherReportList(Round round, String excludeHitId) {
//		List<String> reportArray = new ArrayList<String>();
//		for (String hitId: this.playerHitIds) {
//			if (hitId.equals(excludeHitId))
//				continue;
//			reportArray.add(round.getReport(hitId));
//		}
//		return reportArray;
//	}

//	public List<String> getOtherHitIds(String excludeHitId) {
//
//		List<String> otherHitIds = new ArrayList<String>();
//		for (String innerHitId: this.playerHitIds) {
//			if (innerHitId.equals(excludeHitId))
//				continue;
//			else
//				otherHitIds.add(innerHitId);
//		}
//		return otherHitIds;
//	}

//	public List<String> getRefReports(String excludeHitId, int roundIndex) {		
//		List<String> refReports = new ArrayList<String>();
//		for (String hitId: this.playerHitIds) {
//			if (hitId.equals(excludeHitId))
//				continue;
//			refReports.add( this.rounds.get(roundIndex).getReport(hitId));
//		}
//		return refReports;
//	}

//	public String getRefReport(int roundIndex, String hitId) {
//		Round currRound = this.rounds.get(roundIndex);
//
//		Map<String, Map<String, Object>> roundResult = currRound.result;
//		Map<String, Object> myResult = roundResult.get(hitId);
//		
//		String refPlayerHitId = (String) myResult.get("refPlayer");
//		Map<String, Object> refPlayerResult = roundResult.get(refPlayerHitId);
//		
//		String refReport = (String) refPlayerResult.get("report");
//
//		return refReport;
//	}
	
}
