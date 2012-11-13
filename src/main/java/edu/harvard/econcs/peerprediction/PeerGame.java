package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public class PeerGame {

	/**
	 * List of players
	 */
	List<? extends PeerPlayer> players;
	
	/**
	 * Number of rounds
	 */
	int nRounds;
	
	/**
	 * Jar
	 */
	Map<String, Double> probs;
	
	/**
	 * Payment rule
	 */
	PaymentRule paymentRule;
	
	/**
	 * Index of current round
	 */
	AtomicInteger currentRoundNum;
	
	/**
	 * Current round
	 */
	AtomicReference<PeerRound> currentRound;
	
	/**
	 * 
	 */
	List<PeerResult> results;
	
	/*
	 * Constructor
	 */
	public PeerGame(int nRounds, Map<String, Double> probs, PaymentRule paymentRule) {
		
		this.nRounds = nRounds;
		this.probs = probs;
		this.paymentRule = paymentRule;
		
		currentRound = new AtomicReference<PeerRound>(null);
		currentRoundNum = new AtomicInteger();
		
		results = new ArrayList<PeerResult>();
	}
	
	/**
	 * Start the game
	 * @param players
	 */
	public void startGame(List<? extends PeerPlayer> players) {
		
		// get the set of players
		this.players = players;
		
		// send each player general information
		double[] paymentArray = paymentRule.getPaymentArray();
		for (PeerPlayer p: players) {
			p.sendGeneralInfo(nRounds, players.size(), paymentArray);
		}
		
		// set current round number and create current round
		currentRoundNum.set(1);
		PeerRound r = new PeerRound(this, probs, paymentRule);
		currentRound.set(r);
		
		// start current round
		r.startRound();
		System.out.printf("\n\nGame:\t Started round %d\n", currentRoundNum.get());
	}
	
	/**
	 * Called when a round is completed
	 */
	public void roundCompleted() {
		
		if (!currentRound.get().isCompleted()) {
			System.out.println("Error: trying to start next round before current one is completed");
			return;
		}

		// store the results
		this.results.add(currentRound.get().getResult());
		
		if (currentRoundNum.incrementAndGet() > nRounds) {
			
			// Send players to debrief
			System.out.println("\nGame:\t All games are finished");
			
		} else {
			
			// create a new round
			PeerRound r = new PeerRound(this, probs, paymentRule);
			currentRound.set(r);

			r.startRound();
			System.out.printf("\n\nGame:\t Started round %d\n", currentRoundNum.get());
		}
	}

	/**
	 * When a report is received, notify the current round
	 * @param reporter
	 * @param report
	 */
	public void reportReceived(TestPlayer reporter, String report) {
		currentRound.get().reportReceived(reporter, report);
	}

	
}
