package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.CombinedHITWorkerGroup;
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
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;

@ExperimentServer("Peer Prediction Game")
public class PeerGame {

	int nRounds;	
	PeerPrior prior;
	PaymentRule paymentRule;
	List<PeerResult> results;	
	AtomicReference<PeerRound> currentRound;
	
	String[] playerNames;
	
	HITWorkerGroup realWorkerGroup;
	ExperimentLog expLog;
	ExperimentController controller;
	
	HITWorkerGroup combinedGroup;
	
	ConcurrentHashMap<String, HITWorker> disconnectedList;
	ConcurrentHashMap<String, HITWorker> killedList;
	
	// kill threshold for disconnected time in milliseconds: 1 minute for now
	static final long killThreshold = 1000 * 60;
//	static final long killThreshold = 1000 * 60 * 60;  // For Testing
	
	@Inject
	public PeerGame(
			HITWorkerGroup group,
			ExperimentLog expLog,
			ExperimentController controller) {
		
		this.realWorkerGroup = group;
		this.expLog = expLog;
		this.controller = controller;		
		
		this.disconnectedList = new ConcurrentHashMap<String, HITWorker>();
		this.killedList = new ConcurrentHashMap<String, HITWorker>();
	}
	
	/**
	 * Init with no fake players
	 * @param nRounds
	 * @param prior
	 * @param rule
	 */
	public void init(int nRounds, PeerPrior prior, PaymentRule rule) {
		this.init(nRounds, prior, rule, null);
	}
	
	public void init(int nRounds, PeerPrior prior, PaymentRule rule, 
			FakeHITWorkerGroup fakePlayers) {
		this.nRounds = nRounds;
		this.prior = prior;
		this.paymentRule = rule;		
		
		// For introducing fake players
		this.combinedGroup = fakePlayers == null ? realWorkerGroup : new CombinedHITWorkerGroup(realWorkerGroup, fakePlayers);
		
		currentRound = new AtomicReference<PeerRound>();		

		results = new ArrayList<PeerResult>();
	}
	
	@StartExperiment
	public void startGame() {
		int numPlayers = combinedGroup.groupSize();		
		playerNames = new String[numPlayers];
		combinedGroup.getHITIds().toArray(playerNames);

		expLog.printf("Prior is %s", prior.toString());
		expLog.printf("General information sent: numPlayers=%d, numRounds=%s, " +
				"playerNames=%s, paymentRule=%s, signalList=%s",  
				numPlayers, nRounds, Arrays.toString(playerNames), 
				Arrays.toString(paymentRule.getPaymentArray()), 
				Arrays.toString(prior.getSignalArray()));
		
		for (HITWorker worker : combinedGroup.getHITWorkers()) {
			PlayerUtils.sendGeneralInfo(
					worker, 
					numPlayers, 
					nRounds, 
					playerNames,
					worker.getHitId(),  
					paymentRule.getPaymentArray(), 
					prior.getSignalArray());
		}

		controller.startRounds();
	}

	@StartRound
	public void startRound(int round) {
		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		expLog.printf("Chosen world is %s", chosenWorld.toString());
		
		PeerRound r = new PeerRound(combinedGroup, chosenWorld, paymentRule, expLog);
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
			
			// set bonus amounts for workers
			for (HITWorker worker : combinedGroup.getHITWorkers()) {
				// Do not set bonus if worker is already killed.
				if (killedList.containsKey(worker.getHitId())) {
					expLog.printf("Worker %s killed, no bonus", worker);
					continue;
				}
				double total = 0.0;
				for (PeerResult res : this.results) {
					total += Double.parseDouble(res.getReward(worker));
				}
				double avg = total / nRounds;
				
				// only set bonus amount if the worker is real, not fake
				if( realWorkerGroup.contains(worker)) {
					controller.setBonusAmount(worker, avg);
					expLog.printf("Worker %s gets bonus %.2f", worker, avg);
				}
				
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
				.reportReceived(worker, report, true)) {			
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
			// In this case, the client will received a experiment completed message,
			// and it will automatically load the exit survey
		} else {
			List<Map<String, Map<String, String>>> existingResults = PeerResult
					.getAllResultsForWorker(results, worker);

			currentRound.get().resendState(worker, combinedGroup.groupSize(), nRounds,
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
				expLog.printf("Worker %s killed, because disconnected for %d milliseconds", worker, worker.getDisconnectedTime());
			}
		}
	}
	
	@IntervalEvent(interval=10, unit=TimeUnit.SECONDS) // Expected time to make move: 5 seconds
	public void makeFakeMoves() {
		
		// for each worker in killed hashmap, put in a move
		for (String hitId : killedList.keySet()) {
			HITWorker worker = killedList.get(hitId);
			if (!this.currentRound.get().getResult().containsReport(worker)) {
				String signal = currentRound.get().getResult().getSignal(worker);
				boolean isReal = false;
				boolean roundFinished = currentRound.get().reportReceived(worker, signal, isReal);
				if ( roundFinished ) {
					roundCompleted();
				}
			}
		}
		
	}
	
	@TimeLimit
	public void outOfTime() {
		// Force end of experiment if people take too long
	}

}
