package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.DatabaseType;
import edu.harvard.econcs.turkserver.config.ExperimentType;
import edu.harvard.econcs.turkserver.config.HITCreation;
import edu.harvard.econcs.turkserver.config.LoggingType;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestServerModule;
import edu.harvard.econcs.turkserver.server.ClientGenerator;

import edu.harvard.econcs.turkserver.server.TurkServer;

public class SingleUserServerTest {

	static final int groupSize = 2;
	static final int nRounds = 6;
	
	static final int fakeWorkers = 0;
	static final int totalHITs = 2;
	
	static class TestModule extends TestServerModule {	
		
		@Override
		public void configure() {
			super.configure();
								
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new SimpleConfigurator(nRounds, groupSize));	
			bindString(TSConfig.EXP_SETID, "test set");
		}		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DataModule dm = new DataModule();
		dm.setHITLimit(totalHITs);
		
		TurkServer ts = new TurkServer(dm);
		
		ts.runExperiment(
				new TestModule(),
				ExperimentType.GROUP_EXPERIMENTS,
				DatabaseType.TEMP_DATABASE,
				HITCreation.NO_HITS,
				LoggingType.SCREEN_LOGGING
				);

		Thread.sleep(1000);
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < fakeWorkers; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
		
	}

}
