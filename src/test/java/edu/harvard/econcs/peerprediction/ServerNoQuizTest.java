package edu.harvard.econcs.peerprediction;

import java.util.Collections;
import java.util.List;
import java.util.Set;

import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.ConfigModules;
import edu.harvard.econcs.turkserver.config.ServerModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.config.TestConfigModules;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.server.TurkServer;

public class ServerNoQuizTest {

	static final int groupSize = 3;
	static final int nRounds = 10;
	
	static final int fakeWorkers = 2;
	
	static final double passRate = 0.8;
	static final int maxTries = 3;
	
	static class TestModule extends ServerModule {	
		
		@Override
		public void configure() {
			super.configure();
			
			// No quiz
			bind(QuizFactory.class).toProvider(Providers.of((QuizFactory) null));
			bind(QuizPolicy.class).toProvider(Providers.of((QuizPolicy) null));					
//			bind(QuizFactory.class).toInstance(new QuizFactory.NullQuizFactory());
//			bind(QuizPolicy.class).toInstance(new QuizPolicy.PercentageQuizPolicy(passRate, maxTries));
			
			bindResources(new Resource[] { });
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));
								
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new SimpleConfigurator(nRounds, groupSize));	
			bindString(TSConfig.EXP_SETID, "test set");
		}		
	}
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DataModule dm = new DataModule("testing.properties");
		
		TurkServer ts = new TurkServer(dm);
		
		ts.runExperiment(
				new TestModule(),
				ConfigModules.GROUP_EXPERIMENTS,
				TestConfigModules.TEMP_DATABASE,
				TestConfigModules.NO_HITS,
				TestConfigModules.SCREEN_LOGGING
				);

		Thread.sleep(1000);
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < fakeWorkers; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
		
		ts.awaitTermination();
		ts.disposeGUI();
		
		cg.disposeAllClients();
	}

}
