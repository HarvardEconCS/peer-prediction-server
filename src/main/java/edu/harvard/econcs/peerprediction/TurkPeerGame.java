package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.harvard.econcs.turkserver.api.*;

import com.google.inject.Inject;

public class TurkPeerGame extends PeerGame<TurkPeerPlayer> {
	
	Map<HITWorker, TurkPeerPlayer> workerMap;
	
	public TurkPeerGame(int nRounds2,
			PeerPrior prior, PaymentRule rule) {
		super(nRounds2, prior, rule);
	}

	@Inject
	public void initPlayers(List<HITWorker> players) {
		
		workerMap = new HashMap<HITWorker, TurkPeerPlayer>();
		
		for( HITWorker p : players ) {
			workerMap.put(p, new TurkPeerPlayer(p));
		}		
	
		super.setPlayers(new ArrayList<TurkPeerPlayer>(workerMap.values()));
	}
	
	@ServiceMessage(keys="report")
	public void getReport(HITWorker worker, Map<String, Object> data) {
		
		TurkPeerPlayer player = workerMap.get(worker);
		
		super.reportReceived(player, data.get("report").toString());
		
	}
	
}
