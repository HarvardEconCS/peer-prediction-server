package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

public class PeerResult {

	/**
	 * the chosen world
	 */
	private Map<String, Double> chosenWorld;
	
	/**
	 * Players' signals
	 */
	private HashMap<PeerPlayer, String> signals;
	
	/** 
	 * Players' reports
	 */
	private HashMap<PeerPlayer, String> reports;

	/**
	 * Players' reference players
	 */
	private Map<PeerPlayer, PeerPlayer> refPlayers;
	
	/**
	 * Players' rewards
	 */
	private Map<PeerPlayer, Double> rewards;

//	private Map<String, Map<String, Double>> results;
	
	/**
	 * 
	 * @param chosenWorld
	 */
	public PeerResult(Map<String, Double> chosenWorld) {
		
//		this.results = new HashMap<String, Map<String, Double>>();
//		results.put("chosenWorld", new HashMap<String, Double>());
//		results.put("signals", new HashMap<String, Double>());
//		results.put("reports", new HashMap<String, Double>());
//		results.put("refPlayers", new HashMap<String, Double>());
//		results.put("rewards", new HashMap<String, Double>());

		this.chosenWorld = chosenWorld;
		signals = new HashMap<PeerPlayer, String>();
		reports = new HashMap<PeerPlayer, String>();
		refPlayers = new HashMap<PeerPlayer, PeerPlayer>();
		rewards = new HashMap<PeerPlayer, Double>();
	}

	
	public void recordSignal(PeerPlayer p, String selected) {
		signals.put(p, selected);
	}


	public void recordReport(PeerPlayer reporter, String report) {
		reports.put(reporter, report);
	}


	public void recordRefPlayer(PeerPlayer peerPlayer, PeerPlayer refPlayer) {
		refPlayers.put(peerPlayer, refPlayer);
	}


	public void recordReward(PeerPlayer p, Double reward) {
		rewards.put(p, reward);
	}


	public boolean containsReport(PeerPlayer reporter) {
		return reports.containsKey(reporter);
	}


	public int getReportSize() {
		return reports.size();
	}


	public String getResultForPlayer(PeerPlayer p) {
		
		// TODO: return result as a proper json string
		StringBuilder sb = new StringBuilder();
		sb.append(String.format("chosenWorld: %s", this.chosenWorld.toString()));
		for (PeerPlayer player: signals.keySet()) {
			if (player.name.equals(p.name)) {
				sb.append(String.format("%s: %s, %s, %s, %.2f;", 
						p.name, signals.get(p), reports.get(p), refPlayers.get(p).name, rewards.get(p)));
			} else {
				sb.append(String.format("%s: %s, %s, %.2f;", 
						player.name, reports.get(player), refPlayers.get(player).name, rewards.get(player)));
			}
		}
		return sb.toString();
	}
	
	public void computePayments(PaymentRule paymentRule) {
		
		Random r = new Random();
		List<PeerPlayer> players = new ArrayList<PeerPlayer>();
		players.addAll(signals.keySet());
		
		// Choose reference reports for each player
		for (int i = 0; i < players.size(); i++) {
			int refPlayerIdx = r.nextInt(players.size() - 1);
			if (refPlayerIdx >= i)
				refPlayerIdx++;

			this.recordRefPlayer(players.get(i), players.get(refPlayerIdx));
		}
		
		// Look up payment in the payment table, assign payment to each player
		for (PeerPlayer p : players) {
			PeerPlayer refPlayer = refPlayers.get(p);
			String myReport = reports.get(p);
			String otherReport = reports.get(refPlayer);
			Double reward = paymentRule.getPayment(myReport, otherReport);
			
			this.recordReward(p, reward);
		}
	}
	
	
	
}
