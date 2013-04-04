package edu.harvard.econcs.peerprediction;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.Configuration;
import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.client.LobbyClient;

import edu.harvard.econcs.turkserver.config.*;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.server.TurkServer;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

public class GroupsTest {

	static final String configFile = "testing.properties";
	
	static final int groupSize = 3;
	static final int nRounds = 3;
	static final int totalHITs = 15;
	
	static class TestModule extends ServerModule {		
		
		@Override
		public void configure() {
			super.configure();						
						
			// No quiz
			bind(QuizFactory.class).toProvider(Providers.<QuizFactory>of(null));
			bind(QuizPolicy.class).toProvider(Providers.<QuizPolicy>of(null));			
			
			bindResources(new Resource[] {});
			
			bind(new TypeLiteral<List<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_SPECIAL_WORKERS)).toInstance(new LinkedList<String>());
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));	
						
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new ConfiguratorNoFakePlayers(nRounds, groupSize));	
			bindString(TSConfig.EXP_SETID, "test set");
		}		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DataModule dataModule = new DataModule(configFile);				
		
		// Replace config file values for this test
		Configuration conf = dataModule.getConfiguration();
		conf.setProperty(TSConfig.SERVER_HITGOAL, totalHITs);						
		conf.setProperty(TSConfig.EXP_REPEAT_LIMIT, 1);
		conf.setProperty(TSConfig.SERVER_DEBUGMODE, true);		
		
		TestModule module = new TestModule();
		
		// Create (or empty) database
		MySQLDataTracker.createSchema(dataModule.getConfiguration());
		
		TurkServer ts = new TurkServer(dataModule);
		
		ts.runExperiment(
				module,
				ConfigModules.MYSQL_DATABASE,
				ConfigModules.PERSIST_LOGGING,
				ConfigModules.GROUP_EXPERIMENTS,
				TestConfigModules.NO_HITS
				);
		
		Thread.sleep(1000);
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < totalHITs; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
						
		ts.awaitTermination();
		ts.disposeGUI();
		
		cg.disposeAllClients();
	}

}
