package edu.harvard.econcs.peerprediction;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.server.FakeHITWorker;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

class ConfiguratorWithFakePlayers implements Configurator {
	
	final int nrounds, groupSize;
	ExecutorService exeService;
	
	ConfiguratorWithFakePlayers(int nrounds, int groupSize) {
		this.nrounds = nrounds;
		this.groupSize = groupSize;
		
		exeService = Executors.newCachedThreadPool();
	}
	
	@Override
	public String configure(Object experiment, String expId, HITWorkerGroup group) {
		PeerGame game = (PeerGame) experiment;			
		
		int numFakePlayers = 6;
		
		try {
			FakeHITWorkerGroup fakePlayers  = TestUtils.getFakeGroup("", numFakePlayers, TestPlayer.class);						  
			
			for(HITWorker fakePlayer: fakePlayers.getHITWorkers()) {
				FakeHITWorker fakeHITWorker = (FakeHITWorker) fakePlayer;
				TestPlayer player = (TestPlayer) fakeHITWorker.getClientBean();
				player.overrideConfirmationSignal(fakeHITWorker, game);
				exeService.submit(player);
			}
			
			game.init(nrounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule(), fakePlayers);

		} catch (Exception e) {
			e.printStackTrace();
		}
		
//		game.init(nrounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
		
		return "threereal-sixfake";
	}

	@Override
	public int groupSize() {			
		return groupSize;
	}		
}