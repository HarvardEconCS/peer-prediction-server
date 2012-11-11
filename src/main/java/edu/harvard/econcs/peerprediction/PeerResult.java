package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.ConcurrentHashMap;

public class PeerResult {
	
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

	
	public PeerResult(PeerGame game) {
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
		return null;
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
