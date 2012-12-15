package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.Configurator;
import edu.harvard.econcs.turkserver.client.ClientUtils;
import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.server.TSConfig;
import edu.harvard.econcs.turkserver.server.TurkServer;
import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;

public class SingleUserServerTest {

	static final int groupSize = 3;
	static final int nRounds = 3;
	
	static class SingleUserConfigurator implements Configurator {
		@Override
		public void configure(Object experiment, String inputData) {
			PeerGame game = (PeerGame) experiment;			
			game.init(nRounds, PeerPrior.getTestPrior(), PaymentRule.getTestPaymentRule());
		}

		@Override
		public int groupSize() {			
			return groupSize;
		}		
	}
	
	static class TestModule extends TSTestModule {		
		@Override
		public void configure() {
			super.configure();
			
			bindGroupExperiments();					
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new SingleUserConfigurator());	
			bindString(TSConfig.EXP_SETID, "test set");
		}		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {	
		TurkServer.testExperiment(new TestModule());

		Thread.sleep(1000);
		
		LobbyClient<TestPlayer> client1 = ClientUtils.getWrappedLobbyClient(TestPlayer.class);
		LobbyClient<TestPlayer> client2 = ClientUtils.getWrappedLobbyClient(TestPlayer.class);
		LobbyClient<TestPlayer> client3 = ClientUtils.getWrappedLobbyClient(TestPlayer.class);
		
		client1.connect("http://localhost:9876/cometd/", "HIT 1", "Assignment 1", "Worker 1");
		client2.connect("http://localhost:9876/cometd/", "HIT 2", "Assignment 2", "Worker 2");
		client3.connect("http://localhost:9876/cometd/", "HIT 3", "Assignment 3", "Worker 3");
		
		client1.getClientBean().run();
		client2.getClientBean().run();
		client3.getClientBean().run();
	}

}
