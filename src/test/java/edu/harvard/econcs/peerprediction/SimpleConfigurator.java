package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.Configurator;

class SimpleConfigurator implements Configurator {
	
	final int nrounds, groupSize;
	
	SimpleConfigurator(int nrounds, int groupSize) {
		this.nrounds = nrounds;
		this.groupSize = groupSize;
	}
	
	@Override
	public void configure(Object experiment, String inputData) {
		PeerGame game = (PeerGame) experiment;			
		game.init(nrounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
	}

	@Override
	public int groupSize() {			
		return groupSize;
	}		
}