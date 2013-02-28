package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.ExperimentServer;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.api.IntervalEvent;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;
import edu.harvard.econcs.turkserver.api.WorkerConnect;
import edu.harvard.econcs.turkserver.api.WorkerDisconnect;

@ExperimentServer("Peer Prediction Game")
public class PeerGame {

	int nRounds;	
	PeerPrior prior;
	PaymentRule paymentRule;
	List<PeerResult> results;	
	AtomicReference<PeerRound> currentRound;
	
	String[] playerNames;
	
	HITWorkerGroup group;
	ExperimentLog expLog;
	ExperimentController controller;
	
	@Inject
	public PeerGame(
			HITWorkerGroup group,
			ExperimentLog expLog,
			ExperimentController controller) {
		
		this.group = group;
		this.expLog = expLog;
		this.controller = controller;			
	}
	
	public void init(int nRounds2, PeerPrior prior, PaymentRule rule) {
		this.nRounds = nRounds2;
		this.prior = prior;
		this.paymentRule = rule;		
		
		currentRound = new AtomicReference<PeerRound>();		

		results = new ArrayList<PeerResult>();
	}
	
	@StartExperiment
	public void startGame() {
		int numPlayers = group.groupSize();
		
		System.out.println("GROUP: " + group);
		
		playerNames = new String[numPlayers];
		group.getHITIds().toArray(playerNames);

		for (HITWorker p : group.getHITWorkers()) {
			PlayerUtils.sendGeneralInfo(p, 
					numPlayers, 
					nRounds, 
					playerNames,
					p.getHitId(),  
					paymentRule.getPaymentArray(), 
					prior.getSignalArray());
		}
		
		controller.startRounds();
	}

	@StartRound
	public void startRound(int round) {
		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		PeerRound r = new PeerRound(group, chosenWorld, paymentRule, expLog);
		currentRound.set(r);

		r.startRound();
	}		

	public void roundCompleted() {
		
		if (!currentRound.get().isCompleted()) {
			expLog.printf("Error: trying to start next round before current one is completed");
			return;
		}

		// store the results
		this.results.add(currentRound.get().getResult());				
		
		if (controller.getCurrentRound() == nRounds) {
			expLog.printf("PeerGame: finish experiment");
			
			// set bonus amounts for workers
			for (HITWorker worker : group.getHITWorkers()) {
				double bonus = 0.0;
				for (PeerResult res : this.results) {
					bonus += Double.parseDouble(res.getReward(worker));
				}
				controller.setBonusAmount(worker, bonus);
			}

			
			controller.finishExperiment();
		} else {
			controller.finishRound();			
		}
	}
	
	@ServiceMessage(key="report")
	public void reportReceived(HITWorker worker, Map<String, Object> data) {
		String report = data.get("report").toString();
		if (currentRound.get()
				.reportReceived(worker, report)) {			
			roundCompleted();
		}
	}
	
	@WorkerConnect
	public void workerReconnect(HITWorker worker) {
		PlayerUtils.sendGeneralInfo(worker, group.groupSize(), nRounds, playerNames,
				worker.getHitId(), paymentRule.getPaymentArray(), prior.getSignalArray());
		
		currentRound.get().resendState(worker);
	}
	
	@WorkerDisconnect
	public void workerDisconnect(HITWorker worker) {
		// TODO check if we should end the game prematurely, update client interfaces
	}
	
	@IntervalEvent(interval=500, unit=TimeUnit.MILLISECONDS)
	public void bookkeeping() {
		
	}
	
	@TimeLimit
	public void outOfTime() {
		// Force end of experiment if people take too long
	}

}
