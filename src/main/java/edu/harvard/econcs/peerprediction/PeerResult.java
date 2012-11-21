package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PeerResult {

	private Map<String, Map<String, String>> resultObject;
	private Map<String, Double> chosenWorld;
	
	public PeerResult(Map<String, Double> chosenWorld) {
		
		this.chosenWorld = chosenWorld;
		this.resultObject = new HashMap<String, Map<String, String>>();
		
	}

	public void saveSignal(PeerPlayer p, String selected) {
		if (resultObject.containsKey(p.name)) {
			Map<String, String> playerResult = resultObject.get(p.name);
			playerResult.put("signal", selected);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("signal", selected);
			resultObject.put(p.name, playerResult);
		}

	}


	public void saveReport(PeerPlayer reporter, String report) {
		if (resultObject.containsKey(reporter.name)) {
			Map<String, String> playerResult = resultObject.get(reporter.name);
			playerResult.put("report", report);
		} else {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", report);
			resultObject.put(reporter.name, playerResult);
		}

	}

	public boolean containsReport(PeerPlayer reporter) {
		return resultObject.containsKey(reporter.name) 
				&& resultObject.get(reporter.name).containsKey("report");
	}


	public int getReportSize() {
		int numReports = 0;
		for (String key : this.resultObject.keySet()) {
			if (resultObject.get(key).containsKey("report"))
				numReports++;
		}
		return numReports;
	}

	public Map<String, Map<String, String>> getResultForPlayer(PeerPlayer p) {

		Map<String, Map<String, String>> currResult = new HashMap<String, Map<String, String>>();
		
		for (String playerName: resultObject.keySet()) {
			Map<String, String> playerResult = new HashMap<String, String>();
			playerResult.put("report", resultObject.get(playerName).get("report"));
			playerResult.put("refPlayer", resultObject.get(playerName).get("refPlayer"));
			currResult.put(playerName, playerResult);
		}
		return currResult;
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
			Double reward = paymentRule.getPayment(myReport, otherReport);
			
			Map<String, String> playerResult = resultObject.get(playerName);
			playerResult.put("reward", String.format("%f", reward));
		}
	}
	
	
	
}
