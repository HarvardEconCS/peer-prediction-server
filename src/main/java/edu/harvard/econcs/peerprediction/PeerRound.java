package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import net.andrewmao.math.RandomSelection;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

public class PeerRound {

	private HITWorkerGroup group;
	private PaymentRule paymentRule;
	private Map<String, Double> chosenWorld;
	private PeerResult result;

//	private volatile boolean isStarted = false;	
	private volatile boolean isCompleted = false;
	private ExperimentLog expLog;

	public PeerRound(
			HITWorkerGroup group, 
			Map<String, Double> chosenWorld,
			PaymentRule paymentRule, ExperimentLog expLog) {
		
		this.group = group;
		this.chosenWorld = chosenWorld;
		this.paymentRule = paymentRule;
		this.expLog = expLog;

		result = new PeerResult(this.chosenWorld);
	}

	/**
	 * Start the round
	 */
	public void startRound() {
		
//		isStarted = true;
		
		for (HITWorker worker : group.getHITWorkers()) {

			String chosenSignal = this.chooseSignal();			
			result.saveSignal(worker, chosenSignal);
			
			PlayerUtils.sendSignal(worker, chosenSignal);
			expLog.printf("Worker %s got signal %s", worker, chosenSignal);
		}
	}

	/**
	 * @param reporter
	 * @param report
	 * @param randomRadio 
	 * @param isReal 
	 * @return true if all reports have been received for the round
	 */
	public boolean reportReceived(HITWorker reporter, String report, String randomRadio, boolean isReal) {		
		if (isReal)
			expLog.printf("Worker %s chose report %s (radio: %s)", reporter, report, randomRadio);
		else
			expLog.printf("Worker %s killed, put in fake report %s", reporter, report);
		
		if (result.containsReport(reporter)) {
			expLog.printf("Warning: %s already reported this round", reporter);
			return isCompleted;
		}

		result.saveReport(reporter, report);

		for (HITWorker p : group.getHITWorkers()) {
			PlayerUtils.sendReportConfirmation(p, reporter.getHitId());
		}

		// TODO deal with synchronization issues
		if (result.getReportSize() == group.groupSize()) {
			computePayments();			
		}
		
		return isCompleted;
	}

	/**
	 * Resend state when worker reconnects to game
	 * @param worker
	 * @param groupSize
	 * @param nRounds
	 * @param playerNames
	 * @param hitId
	 * @param paymentArray
	 * @param signalArray
	 * @param existingResults
	 */
	public void resendState(HITWorker worker, int groupSize, int nRounds,
			String[] playerNames, String hitId, double[] paymentArray,
			String[] signalArray,
			List<Map<String, Map<String, String>>> existingResults) {
		
		List<String> workersConfirmed = new ArrayList<String>();
		for (HITWorker p : group.getHITWorkers()) {
			if (result.getReport(p) != null)
				workersConfirmed.add(p.getHitId());
		}
		String currPlayerSignal = result.getSignal(worker);
		String currPlayerReport = result.getReport(worker);
		
		PlayerUtils.resentState(worker, groupSize, nRounds, playerNames, 
				hitId, paymentArray, signalArray, 
				existingResults, 
				currPlayerSignal, currPlayerReport, workersConfirmed);
	
	}


	/**
	 * Resend state when worker reconnects to game
	 * @param worker
	 * @param groupSize
	 * @param nRounds
	 * @param playerNames
	 * @param hitId
	 * @param paymentArray
	 * @param signalArray
	 * @param existingResults
	 */
	public void resendState(HITWorker worker, int groupSize, int nRounds,
			String[] playerNames, String hitId, double[][] paymentArray,
			String[] signalArray,
			List<Map<String, Map<String, String>>> existingResults) {
		
		List<String> workersConfirmed = new ArrayList<String>();
		for (HITWorker p : group.getHITWorkers()) {
			if (result.getReport(p) != null)
				workersConfirmed.add(p.getHitId());
		}
		String currPlayerSignal = result.getSignal(worker);
		String currPlayerReport = result.getReport(worker);
		
		PlayerUtils.resentState(worker, groupSize, nRounds, playerNames, 
				hitId, paymentArray, 
				signalArray, 
				existingResults, 
				currPlayerSignal, currPlayerReport, workersConfirmed);
	
	}
	
	/**
	 * Choose signal given the chosen world
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

	/**
	 * Compute payments
	 */
	private void computePayments() {
		result.computePayments(this.paymentRule);
		expLog.printf("Round result is %s", result.toString());

		for (HITWorker p : group.getHITWorkers()) {
			// TODO:  where should this happen?
			Map<String, Map<String, String>> resultForPlayer = result.getResultForPlayer(p);
			PlayerUtils.sendResults(p, resultForPlayer);
		}
		
		isCompleted = true;
	}


	public boolean isCompleted() {
		return isCompleted;
	}

	public PeerResult getResult() {
		return this.result;
	}
}
