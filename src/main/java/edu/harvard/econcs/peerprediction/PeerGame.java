package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
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
	
	ConcurrentHashMap<String, HITWorker> disconnectedList;
	ConcurrentHashMap<String, HITWorker> killedList;
	
	// kill threshold for disconnected time in milliseconds: 1 minute for now
	static final long killThreshold = 1000 * 20;
//	static final long killThreshold = 1000 * 60 * 60;  // For Testing
	
	@Inject
	public PeerGame(
			HITWorkerGroup group,
			ExperimentLog expLog,
			ExperimentController controller) {
		
		this.group = group;
		this.expLog = expLog;
		this.controller = controller;		
		
		this.disconnectedList = new ConcurrentHashMap<String, HITWorker>();
		this.killedList = new ConcurrentHashMap<String, HITWorker>();
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
				double total = 0.0;
				for (PeerResult res : this.results) {
					total += Double.parseDouble(res.getReward(worker));
				}
				double avg = total / nRounds;
				controller.setBonusAmount(worker, avg);
				expLog.printf("PeerGame: bonus amount for %s is %.2f", worker, avg);
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
		
		// if worker is in killed hashmap, send error
		if (killedList.containsKey(worker.getHitId())) {
			// send error
			PlayerUtils.sendDisconnectedErrorMessage(worker);
			return;
		}

		// if worker is in disconnected hashmap, take out
		if (disconnectedList.containsKey(worker.getHitId()))
			disconnectedList.remove(worker.getHitId());
		
		if (results.size() == nRounds) {
			// TODO:  This case does not seem to be handled here.
		} else {
			List<Map<String, Map<String, String>>> existingResults = PeerResult
					.getAllResultsForWorker(results, worker);

			currentRound.get().resendState(worker, group.groupSize(), nRounds,
					playerNames, worker.getHitId(),
					paymentRule.getPaymentArray(), prior.getSignalArray(),
					existingResults);
		}
	}
	
	@WorkerDisconnect
	public void workerDisconnect(HITWorker worker) {
		// put worker in disconnected hashmap
		this.disconnectedList.put(worker.getHitId(), worker);
	}
	
	@IntervalEvent(interval=500, unit=TimeUnit.MILLISECONDS)
	public void killWorkers() {
		
		// for each worker in disconnected hashmap, 
		for (String hitId : disconnectedList.keySet()) {
			HITWorker worker = disconnectedList.get(hitId);
			
			// if they have been disconnected more than threshold, put in killed hashmap
			if (worker.getDisconnectedTime() > killThreshold) {
				disconnectedList.remove(hitId);
				killedList.put(hitId, worker);
				expLog.printf("PeerGame: killed %s because disconnected for too long", worker);
			}
		}
	}
	
	@IntervalEvent(interval=10, unit=TimeUnit.SECONDS) // Expected time to make move: 5 seconds
	public void makeFakeMoves() {
		// for each worker in killed hashmap, put in a move
		for (String hitId : killedList.keySet()) {
			HITWorker worker = killedList.get(hitId);
			// The fake player is always honest
			String signal = this.currentRound.get().getResult().getSignal(worker);
			this.currentRound.get().reportReceived(worker, signal);
			expLog.printf("PeerGame: fake signal %s for killed worker %s", signal, worker);
		}
		
	}
	
	@TimeLimit
	public void outOfTime() {
		// Force end of experiment if people take too long
	}

}
