package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;

class SimpleConfigurator implements Configurator {
	
	final int nrounds, groupSize;
	
	SimpleConfigurator(int nrounds, int groupSize) {
		this.nrounds = nrounds;
		this.groupSize = groupSize;
	}
	
	@Override
	public String configure(Object experiment, String expId, HITWorkerGroup group) {
		PeerGame game = (PeerGame) experiment;			
		game.init(nrounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
		
		return "test-treatment";
	}

	@Override
	public int groupSize() {			
		return groupSize;
	}		
}