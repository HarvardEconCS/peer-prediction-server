package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.TSBaseModule.TSTestModule;
import edu.harvard.econcs.turkserver.server.TSConfig;
import edu.harvard.econcs.turkserver.server.TurkServer;

public class GroupsTest {

	static final int groupSize = 3;
	static final int nRounds = 3;
	
	static final int totalHITs = 30;
	
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
		
		for( int i = 0; i < totalHITs; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
		
	}

}
