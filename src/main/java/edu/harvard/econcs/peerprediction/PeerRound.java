package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Map.Entry;
import java.util.Random;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

import net.andrewmao.math.RandomSelection;

public class PeerRound {

	private HITWorkerGroup group;
	private PaymentRule paymentRule;
	private Map<String, Double> chosenWorld;
	private PeerResult result;

	private volatile boolean isStarted = false;	
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
		
		for (HITWorker p : group.getHITWorkers()) {

			String selected = this.chooseSignal();			
			result.saveSignal(p, selected);
			expLog.printf("Round: chosen signal %s for %s", selected, p);

			PlayerUtils.sendSignal(p, selected);
		}
	}

	/**
	 * @param reporter
	 * @param report
	 * @return true if all reports have been received for the round
	 */
	public boolean reportReceived(HITWorker reporter, String report) {		
		expLog.printf("Round: received report %s from %s", report, reporter);
		
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

	private void computePayments() {
		result.computePayments(this.paymentRule);
		expLog.printf("Round: result with payment is %s", result.toString());

		for (HITWorker p : group.getHITWorkers()) {
			// TODO:  where should this happen?
			Map<String, Map<String, String>> resultForPlayer = result.getResultForPlayer(p);
			PlayerUtils.sendResults(p, resultForPlayer);
		}
		
		isCompleted = true;
	}


	public PeerResult getResult() {
		return this.result;
	}

	public void resendState(HITWorker worker) {
		String signal = result.getSignal(worker);
		if( signal != null) PlayerUtils.sendSignal(worker, signal );
			
		for (HITWorker p : group.getHITWorkers()) {
			if (p.getHitId().equals(worker.getHitId()))
				continue;
			String report = result.getReport(p);
			if (report != null) PlayerUtils.sendReportConfirmation(worker, p.getHitId());
		}
	}
}
