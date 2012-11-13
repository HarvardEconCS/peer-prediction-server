package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PeerResult {
	
	private Map<String, Double> chosenWorld;
	
	/**
	 * 
	 */
	private HashMap<PeerPlayer, String> signals;
	
	/** 
	 * Players' reports
	 */
	private ConcurrentHashMap<PeerPlayer, String> reports;

	/**
	 * 
	 */
	private Map<PeerPlayer, PeerPlayer> refPlayers;
	
	/**
	 * 
	 */
	private Map<PeerPlayer, Double> rewards;

	
	public PeerResult(Map<String, Double> chosenWorld) {
		this.chosenWorld = chosenWorld;
		signals = new HashMap<PeerPlayer, String>();
		reports = new ConcurrentHashMap<PeerPlayer, String>();
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
		return reports.contains(reporter);
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
