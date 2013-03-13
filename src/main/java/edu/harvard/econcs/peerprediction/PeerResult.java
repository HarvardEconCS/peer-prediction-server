package edu.harvard.econcs.peerprediction;

import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;


import edu.harvard.econcs.turkserver.api.HITWorker;

public class PeerResult {

	Map<String, Map<String, String>> resultObject;
	Map<String, Double> chosenWorld;
	
	public PeerResult(Map<String, Double> chosenWorld) {
		
		this.chosenWorld = chosenWorld;
		this.resultObject = new HashMap<String, Map<String, String>>();
		
	}

	public void saveSignal(HITWorker p, String selected) {
		if (resultObject.containsKey(p.getHitId())) {
			Map<String, String> playerResult = resultObject.get(p.getHitId());
			playerResult.put("signal", selected);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("signal", selected);
			resultObject.put(p.getHitId(), playerResult);
		}

	}


	public void saveReport(HITWorker reporter, String report) {
		if (resultObject.containsKey(reporter.getHitId())) {
			Map<String, String> playerResult = resultObject.get(reporter.getHitId());
			playerResult.put("report", report);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", report);
			resultObject.put(reporter.getHitId(), playerResult);
		}

	}

	public void computePayments(PaymentRule paymentRule) {
		
		Random r = new Random();
		
		List<String> playerNames = new ArrayList<String>();
		playerNames.addAll(this.resultObject.keySet());
		
		// choose reference player
		for (int i = 0; i < playerNames.size(); i++) {
			int refPlayerIdx = r.nextInt(playerNames.size() - 1);
			if (refPlayerIdx >= i)
				refPlayerIdx++;
	
			Map<String, String> playerResult = resultObject.get(playerNames.get(i));
			playerResult.put("refPlayer", playerNames.get(refPlayerIdx));
		}
		
		// find payment
		for (String playerName : playerNames) {
			
			String refPlayerName = resultObject.get(playerName).get("refPlayer");
			String myReport = resultObject.get(playerName).get("report");
			String otherReport = resultObject.get(refPlayerName).get("report");
			double reward = paymentRule.getPayment(myReport, otherReport);
	
			Map<String, String> playerResult = resultObject.get(playerName);
			DecimalFormat df = new DecimalFormat("#.##");
			playerResult.put("reward", df.format(reward));
		}
	}

	public boolean containsReport(HITWorker reporter) {
		return resultObject.containsKey(reporter.getHitId()) 
				&& resultObject.get(reporter.getHitId()).containsKey("report");
	}


	public int getReportSize() {
		int numReports = 0;
		for (String key : this.resultObject.keySet()) {
			if (resultObject.get(key).containsKey("report"))
				numReports++;
		}
		return numReports;
	}

	public String getSignal(HITWorker worker) {
		Map<String, String> result = resultObject.get(worker.getHitId());
		if ( result != null)
			return result.get("signal");
		return null;
	}

	public String getReport(HITWorker worker) {
		Map<String, String> results = resultObject.get(worker.getHitId());
		if (results != null)
			return results.get("report");
		return null;
	}
		
	public String getReward(HITWorker worker) {
		Map<String, String> result = resultObject.get(worker.getHitId());
		if ( result != null)
			return result.get("reward");
		return null;		
	}

	public Map<String, Map<String, String>> getResultForPlayer(HITWorker p) {
	
		Map<String, Map<String, String>> currResult = new HashMap<String, Map<String, String>>();
		
		for (String playerName: resultObject.keySet()) {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", resultObject.get(playerName).get("report"));
			playerResult.put("refPlayer", resultObject.get(playerName).get("refPlayer"));
			playerResult.put("reward", resultObject.get(playerName).get("reward"));
			if (p.getHitId().equals(playerName)) 
				playerResult.put("signal", resultObject.get(playerName).get("signal"));
			currResult.put(playerName, playerResult);
		}
		return currResult;
	}

	public static List<Map<String, Map<String, String>>> getAllResultsForWorker(
			List<PeerResult> results, HITWorker worker) {
		List<Map<String, Map<String, String>>> returnedResults 
			= new ArrayList<Map<String, Map<String, String>>>();
		for (PeerResult res : results) {
			returnedResults.add(res.getResultForPlayer(worker));
		}
		
		return returnedResults;
	}

	@Override
	public String toString() {
		Map<String, String> mapString = new HashMap<String, String>();
		for (String key: this.resultObject.keySet()) {
			mapString.put(key, this.resultObject.get(key).toString());
		}
		return mapString.toString();
	}
	
	
}
