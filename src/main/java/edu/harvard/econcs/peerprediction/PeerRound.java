package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

import net.andrewmao.math.RandomSelection;

public class PeerRound<P extends PeerPlayer> {

	private PeerGame<P> game;
	private PaymentRule paymentRule;
	private Map<String, Double> chosenWorld;
	private PeerResult result;

	private volatile boolean isStarted = false;	
	private volatile boolean isCompleted = false;
	private ExperimentLog expLog;


	public PeerRound(
			PeerGame<P> game, 
			Map<String, Double> chosenWorld,
			PaymentRule paymentRule, ExperimentLog expLog) {
		
		this.game = game;
		this.chosenWorld = chosenWorld;
		this.paymentRule = paymentRule;
		this.expLog = expLog;

		result = new PeerResult(this.chosenWorld);
	}

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

			String selected = this.chooseSignal();
			
			result.saveSignal(p, selected);
			
			p.sendSignal(selected);

		}
	}


	public void reportReceived(PeerPlayer reporter, String report) {		
		expLog.printf("Round:\t received report %s from %s", report, reporter.name);
		
		if (result.containsReport(reporter)) {
			expLog.printf("Warning: %s already reported this round", reporter);
			return;
		}

		result.saveReport(reporter, report);

		for (PeerPlayer p : game.players) {
			p.sendReportConfirmation(reporter);
		}

		// TODO deal with synchronization issues
		if (result.getReportSize() == game.players.size()) {
			computePayments();
		}
	}

	private void computePayments() {

		result.computePayments(this.paymentRule);
		// TODO: Log reference payment and reference player for each payment

		for (PeerPlayer p : game.players) {
			// TODO:  where should this happen?
			Map<String, Map<String, String>> resultForPlayer = result.getResultForPlayer(p);
			p.sendResults(resultForPlayer);
		}
		
		isCompleted = true;
		game.roundCompleted();
	}


	public PeerResult getResult() {
		return this.result;
	}

}
