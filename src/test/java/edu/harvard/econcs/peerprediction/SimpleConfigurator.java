package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.Configurator;

class SimpleConfigurator implements Configurator {
	@Override
	public void configure(Object experiment, String inputData) {
		PeerGame game = (PeerGame) experiment;			
		game.init(SingleUserServerTest.nRounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
	}

	@Override
	public int groupSize() {			
		return SingleUserServerTest.groupSize;
	}		
}