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
	 * Prior
	 */
	PeerPrior prior;

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
	

	/**
	 * Constructor
	 * @param nRounds2
	 * @param prior
	 * @param rule
	 */
	public PeerGame(int nRounds2, PeerPrior prior, PaymentRule rule) {
		this.nRounds = nRounds2;
		this.prior = prior;
		this.paymentRule = rule;
		
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
		double[] paymentArray = paymentRule.getPaymentArray(this.prior.getSignalArray());
		for (PeerPlayer p: players) {
			p.sendGeneralInfo(nRounds, players.size(), paymentArray);
		}
		
		// set current round number and create current round
		currentRoundNum.set(1);
		
		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		PeerRound r = new PeerRound(this, chosenWorld, paymentRule);
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
			Map<String, Double> chosenWorld = this.prior.chooseWorld();
			PeerRound r = new PeerRound(this, chosenWorld, paymentRule);
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
