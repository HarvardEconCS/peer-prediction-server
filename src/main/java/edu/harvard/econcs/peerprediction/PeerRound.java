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
	 * Payment rule
	 */
	private PaymentRule paymentRule;

	/**
	 * Chosen world
	 */
	private Map<String, Double> chosenWorld;
	
	/**
	 * Result of this round
	 */
	private PeerResult result;

	private volatile boolean isStarted = false;
	
	private volatile boolean isCompleted = false;

	/**
	 * Constructor
	 * 
	 * @param game
	 * @param chosenWorld
	 * @param paymentRule
	 */
	public PeerRound(PeerGame game, Map<String, Double> chosenWorld,
			PaymentRule paymentRule) {
		
		this.game = game;
		this.chosenWorld = chosenWorld;
		this.paymentRule = paymentRule;

		result = new PeerResult(this.chosenWorld);
	}

	/**
	 * Choose a signal from the chosenWorld
	 * @return
	 */
	private String chooseSignal() {
		
		String[] signalArray = new String[chosenWorld.size()];
		double[] probArray = new double[chosenWorld.size()];
		int i = 0;
		for (Entry<String, Double> e : chosenWorld.entrySet()) {
			signalArray[i] = e.getKey();
			probArray[i] = e.getValue();
			i++;
		}
		
		Random r = new Random();
		int chosenSignalIdx = RandomSelection.selectRandomWeighted(probArray, r);
		String selectedSignal = signalArray[chosenSignalIdx];
		
		return selectedSignal;
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
			String selected = this.chooseSignal();
			
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

	/**
	 * Getter: result
	 * @return
	 */
	public PeerResult getResult() {
		return this.result;
	}

}
