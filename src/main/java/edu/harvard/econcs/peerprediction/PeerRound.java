package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

public class PeerRound {

	/**
	 * Reference to the game
	 */
	private PeerGame game;

	/**
	 * Jar
	 */
	private Map<String, Double> probs;

	/**
	 * Payment rule
	 */
	private PaymentRule paymentRule;

	/**
	 * Objects, both the signals and the reports
	 */
	private String[] objArray;

	/**
	 * Array of probabilities
	 */
	private double[] probArray;

	/**
	 * 
	 */
	private PeerResult result;

	private Random r;
	
	private volatile boolean isStarted = false, isCompleted = false;

	/**
	 * Constructor
	 * 
	 * @param game
	 * @param probs
	 * @param paymentRule
	 */
	public PeerRound(PeerGame game, Map<String, Double> probs,
			PaymentRule paymentRule) {
		this.game = game;
		this.probs = probs;
		this.paymentRule = paymentRule;

		r = new Random();

		objArray = new String[probs.size()];
		probArray = new double[probs.size()];
		int i = 0;
		for (Entry<String, Double> e : probs.entrySet()) {
			objArray[i] = e.getKey();
			probArray[i] = e.getValue();
			i++;
		}

		result = new PeerResult(game);
		
	}

	public boolean isCompleted() {
		return isCompleted;
	}

	/**
	 * Start the round
	 */
	public void startRound() {
		
		isStarted = true;
		
		for (PeerPlayer p : game.players) {

			// select signal
			int objIdx = RandomSelection.selectRandomWeighted(probArray, r);
			String selected = objArray[objIdx];
			
			// record signal
			result.recordSignal(p, selected);
			
			// send signal to player
			p.sendSignal(selected);

		}
	}

	/**
	 * Called when a report from a player is received
	 * 
	 * @param reporter
	 * @param report
	 */
	public void reportReceived(PeerPlayer reporter, String report) {

		if (result.containsReport(reporter)) {
			System.out.printf("Warning: %s already reported this round", reporter);
			return;
		}

		// record the report received
		result.recordReport(reporter, report);
		System.out.printf("Round:\t received report %s from %s\n", report, reporter.name);

		// send report confirmation message to each player
		for (PeerPlayer p : game.players) {
			p.sendReportConfirmation(reporter);
			System.out.printf("Round:\t sent confirmation of report from %s to %s \n", reporter.name, p.name);
		}

		// TODO deal with synchronization issues
		if (result.getReportSize() == game.players.size()) {
			computePayments();
		}
	}

	/**
	 * Compute and send payments to players
	 */
	private void computePayments() {

		// compute payments
		result.computePayments(this.paymentRule);

		// Send all payments out to players
		for (PeerPlayer p : game.players) {
			String resultForPlayer = result.getResultForPlayer(p);
			p.sendResults(resultForPlayer);
		}

		isCompleted = true;
		game.roundCompleted();
	}

	public PeerResult getResult() {
		return this.result;
	}

}
