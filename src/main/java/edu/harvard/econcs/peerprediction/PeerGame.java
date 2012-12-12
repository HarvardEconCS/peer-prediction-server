package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicReference;

import com.google.inject.Inject;

import edu.harvard.econcs.turkserver.api.Experiment;
import edu.harvard.econcs.turkserver.api.ExperimentController;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.api.ServiceMessage;
import edu.harvard.econcs.turkserver.api.StartExperiment;
import edu.harvard.econcs.turkserver.api.StartRound;
import edu.harvard.econcs.turkserver.api.TimeLimit;
import edu.harvard.econcs.turkserver.api.WorkerConnect;
import edu.harvard.econcs.turkserver.api.WorkerDisconnect;

@Experiment("Peer Prediction Game")
public class PeerGame {

	int nRounds;	

	PeerPrior prior;

	PaymentRule paymentRule;
		
	AtomicReference<PeerRound> currentRound;
	
	List<PeerResult> results;
	
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
		
		String[] playerNames = new String[numPlayers];
		group.getHITIds().toArray(playerNames);
		
		double[] paymentArray = paymentRule.getPaymentArray();

		for (HITWorker p : group.getHITWorkers()) {
			PlayerUtils.sendGeneralInfo(p, numPlayers, nRounds, playerNames, p.getHitId(), paymentArray);
		}
		
		controller.startRounds();
	}

	@StartRound
	public void startRound(int round) {
		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		PeerRound r = new PeerRound(group, chosenWorld, paymentRule, expLog);
		currentRound.set(r);

		r.startRound();
		expLog.printf("Game:\t started round %d", round);
	}		

	public void roundCompleted() {

		expLog.printf("Game:\t round %d completed", controller.getCurrentRound());
		
		if (!currentRound.get().isCompleted()) {
			expLog.printf("Error: trying to start next round before current one is completed");
			return;
		}

		// store the results
		this.results.add(currentRound.get().getResult());
		
		if (controller.getCurrentRound() > nRounds) {
			// Send players to debrief
			expLog.printf("\nGame:\t All games are finished");
			controller.finishExperiment();
		} else {
			// TODO call this asynchronously at some later time
			controller.finishRound();
		}		
	}
	
	@ServiceMessage(key="report")
	public void reportReceived(HITWorker worker, Map<String, Object> data) {
		if (currentRound.get().reportReceived(worker, (String) data.get("report")))			
			roundCompleted();
	}
	
	@WorkerConnect
	public void workerReconnect(HITWorker worker) {
		// TODO send current round state and any received reports
	}
	
	@WorkerDisconnect
	public void workerDisconnect(HITWorker worker) {
		// TODO check if we should end the game prematurely, update client interfaces
	}
	
	@TimeLimit
	public void outOfTime() {
		// Force end of experiment if people take too long
	}

}
