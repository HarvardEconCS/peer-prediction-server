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

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.config.TSBaseModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.server.TurkServer;

public class ServerWithQuizTest {

	static final String configFile = "testing.properties";
	
	static final int groupSize = 2;
	static final int nRounds = 6;
	
	static final int fakeWorkers = 0;
	static final int totalHITs = 2;
	
	static final double passRate = 0.8;
	static final int maxTries = 2;
	
	static class TestModule extends TSBaseModule {
		
		TestModule() throws FileNotFoundException, ConfigurationException {
			super(configFile);
		}
		
		@Override
		public void configure() {
			super.configure();
			
			conf.addProperty(TSConfig.SERVER_HITGOAL, totalHITs);						
			conf.addProperty(TSConfig.EXP_REPEAT_LIMIT, totalHITs);
						
			bindGroupExperiments();					
			bindExperimentClass(PeerGame.class);			
			bindConfigurator(new SimpleConfigurator());	
			bindString(TSConfig.EXP_SETID, "test set");
			
			bindTestClasses();
			
			bind(QuizFactory.class).toInstance(new QuizFactory.NullQuizFactory());
			bind(QuizPolicy.class).toInstance(new QuizPolicy.PercentageQuizPolicy(passRate, maxTries));			
			
			bind(Resource[].class).annotatedWith(Names.named(TSConfig.SERVER_RESOURCES)).toInstance(new Resource[] {});
			
			bind(new TypeLiteral<List<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_SPECIAL_WORKERS)).toInstance(new LinkedList<String>());
			
			bind(new TypeLiteral<Set<String>>() {})
			.annotatedWith(Names.named(TSConfig.EXP_INPUT_LIST)).toInstance(Collections.singleton("test-treatment"));
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {}).toProvider(Providers.of((List<Experiment>) null));		
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
