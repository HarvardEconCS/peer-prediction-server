package edu.harvard.econcs.peerprediction;

import java.io.FileNotFoundException;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Set;

import org.apache.commons.configuration.ConfigurationException;
import org.eclipse.jetty.util.resource.Resource;

import com.google.inject.TypeLiteral;
import com.google.inject.name.Names;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.logging.ExperimentLogImpl;
import edu.harvard.econcs.turkserver.logging.ServerLogController;
import edu.harvard.econcs.turkserver.mturk.HITController;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.config.TSBaseModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.server.TurkServer;
import edu.harvard.econcs.turkserver.server.mturk.FakeHITController;
import edu.harvard.econcs.turkserver.server.mysql.ExperimentDataTracker;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

public class GroupsTest {

	static final String configFile = "testing.properties";
	
	static final int groupSize = 3;
	static final int nRounds = 3;
	
	static final int totalHITs = 30;
	
	static class TestModule extends TSBaseModule {
		
		TestModule() throws FileNotFoundException, ConfigurationException {
			super(configFile);
		}
		
		@Override
		public void configure() {
			super.configure();
						
			conf.addProperty(TSConfig.SERVER_HITGOAL, totalHITs);						
			conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, totalHITs);
			
			bind(ExperimentLog.class).to(ServerLogController.class);
			bind(ServerLogController.class).to(ExperimentLogImpl.class);
			
			bind(HITController.class).to(FakeHITController.class);
			bind(ExperimentDataTracker.class).to(MySQLDataTracker.class);
			
			// No quiz
			bind(QuizFactory.class).toProvider(Providers.of((QuizFactory) null));
			bind(QuizPolicy.class).toProvider(Providers.of((QuizPolicy) null));			
			
			bind(Resource[].class).annotatedWith(Names.named(TSConfig.SERVER_RESOURCES)).toInstance(new Resource[] {});
			
			bind(new TypeLiteral<List<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_SPECIAL_WORKERS)).toInstance(new LinkedList<String>());
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));	
			
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
		TestModule module = new TestModule();
		// Create (or empty) database
		MySQLDataTracker.createSchema(module.getConfiguration());
		
		TurkServer.testExperiment(module);

		Thread.sleep(1000);
		
		ClientGenerator cg = new ClientGenerator("http://localhost:9876/cometd/");
		
		for( int i = 0; i < totalHITs; i++) {
			LobbyClient<TestPlayer> client = cg.getClient(TestPlayer.class);
			new Thread(client.getClientBean()).start();
		}
		
	}

}
