package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Map;

import edu.harvard.econcs.turkserver.api.*;

import com.google.inject.Inject;

public class TurkPeerGame extends PeerGame<TurkPeerPlayer> {
	
	Map<HITWorker, TurkPeerPlayer> workerMap;

	HITWorkerGroup group;
	ExperimentLog expLog;
	ExperimentController controller;
	
	@Inject
	public TurkPeerGame(
			HITWorkerGroup group,
			ExperimentLog expLog,
			ExperimentController controller) {
		
		this.group = group;
		this.expLog = expLog;
		this.controller = controller;
		
		workerMap = new HashMap<HITWorker, TurkPeerPlayer>();
		
		for( HITWorker p : group.getHITWorkers() ) {
			workerMap.put(p, new TurkPeerPlayer(p));
		}		
	
		super.setPlayers(new ArrayList<TurkPeerPlayer>(workerMap.values()));
	}
	
	/*
	 * TODO set this up with some other class
	 */
	public void init(int nRounds2, PeerPrior prior, PaymentRule rule) {
		super.init(nRounds2, prior, rule, expLog);
				
	}	
	
	@Override
	@StartExperiment
	public void startGame() {
		
		controller.start();
		super.startGame();
	}
	
	@ServiceMessage(key="report")
	public void getReport(HITWorker worker, Map<String, Object> data) {
		
		TurkPeerPlayer player = workerMap.get(worker);
		
		String report = data.get("report").toString();
		super.reportReceived(player, report);
				
	}

	@TimeLimit
	public void outOfTime() {
		// Force end of experiment if people take too long
	}
	
	@Override
	public void finishGame() {
		controller.finish();		
	}
	
}
