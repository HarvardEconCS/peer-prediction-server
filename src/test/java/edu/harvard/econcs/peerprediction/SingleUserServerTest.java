package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.server.TurkServer;
import edu.harvard.econcs.turkserver.config.TSBaseModule.TSTestModule;

public class SingleUserServerTest {

	static final int groupSize = 2;
	static final int nRounds = 6;
	
	static final int fakeWorkers = 0;
	static final int totalHITs = 2;
	
	static class TestModule extends TSTestModule {	
		
		@Override
		public void configure() {
			super.configure();
			
			setHITLimit(totalHITs);
			bindGroupExperiments();					
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new SimpleConfigurator());	
			bindString(TSConfig.EXP_SETID, "test set");
		}		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		
		TurkServer.testExperiment(new TestModule());

		Thread.sleep(1000);
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < fakeWorkers; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
		
	}

}
