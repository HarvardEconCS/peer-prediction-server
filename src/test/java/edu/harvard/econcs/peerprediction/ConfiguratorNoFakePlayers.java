package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

class ConfiguratorNoFakePlayers implements Configurator {
	
	final int nrounds, groupSize;
	
	ConfiguratorNoFakePlayers(int nrounds, int groupSize) {
		this.nrounds = nrounds;
		this.groupSize = groupSize;
	}
	
	@Override
	public String configure(Object experiment, String expId, HITWorkerGroup group) {
		PeerGame game = (PeerGame) experiment;			
		
		game.init(nrounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
		
		return "prior2-basic";
	}

	@Override
	public int groupSize() {			
		return groupSize;
	}		
}