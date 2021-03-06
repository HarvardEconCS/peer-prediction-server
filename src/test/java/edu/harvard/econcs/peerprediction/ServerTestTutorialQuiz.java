package edu.harvard.econcs.peerprediction;

import java.util.List;

import org.eclipse.jetty.util.resource.Resource;

import com.amazonaws.mturk.requester.Comparator;
import com.amazonaws.mturk.requester.QualificationRequirement;
import com.google.inject.TypeLiteral;
import com.google.inject.util.Providers;

import edu.harvard.econcs.turkserver.client.LobbyClient;
import edu.harvard.econcs.turkserver.config.ConfigModules;
import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.config.ServerModule;
import edu.harvard.econcs.turkserver.config.TSConfig;
import edu.harvard.econcs.turkserver.mturk.QualMaker;
import edu.harvard.econcs.turkserver.schema.Experiment;
import edu.harvard.econcs.turkserver.server.ClientGenerator;
import edu.harvard.econcs.turkserver.server.QuizFactory;
import edu.harvard.econcs.turkserver.server.QuizPolicy;
import edu.harvard.econcs.turkserver.server.TurkServer;
import edu.harvard.econcs.turkserver.server.mysql.MySQLDataTracker;

public class ServerTestTutorialQuiz {

	static final String configFile = "testing.properties";
	
	static final int groupSize = 4;
	static final int nRounds   = 20;
	
	static final int fakeWorkers = 0;
	
	static final double passRate = 0.8;
	static final int maxTries 	 = 3;
	
	static final String setId = "vary-payment";
	
	static class TestModule extends ServerModule {
		
		@Override
		public void configure() {
			super.configure();
			
			// Configure Quiz
			bind(QuizFactory.class).toInstance(new QuizFactory.StringQuizFactory("tutorial"));
			bind(QuizPolicy.class).toInstance(new QuizPolicy
					.PercentageQuizPolicy(passRate, maxTries));
					
			bindResources(new Resource[] {});			
			
			/*
			try {
				bindResources(new Resource[] {
					Resource.newResource(
						"/home/xagao/workspace_peer/peer-prediction-ui/public")
				});
			} catch (Exception e) {
				throw new RuntimeException(e);
			}
			*/	
			
			// TODO replace this with actual list of past experiments
			bind(new TypeLiteral<List<Experiment>>() {})
				.toProvider(Providers.of((List<Experiment>) null));		
			
			bindExperimentClass(PeerGame.class);	
	
			bindConfigurator(new ConfiguratorNoFakePlayers(nRounds, groupSize));
			
			// Add fake players
//			bindConfigurator(new ConfiguratorWithFakePlayers(nRounds, groupSize));	

			// Set ID
			bindString(TSConfig.EXP_SETID, setId);
			
			// Qualification
			bind(QualificationRequirement[].class).toInstance(new QualificationRequirement[] {
//					QualMaker.getLocaleQual("US"),
//					QualMaker.getMinApprovalRateQual(95)
					QualMaker.getCustomQual("2QYBMJTUHYW25N7F11YSAWM16CQNIL", Comparator.EqualTo, 1)
//					QualMaker.getCustomQual("2MS9RNDWIOV1H1ATD4KGX13YD1QVG4", Comparator.EqualTo, 1)
			});
		}		
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		DataModule dataModule = new DataModule(configFile);
		TurkServer ts = new TurkServer(dataModule);
		
		// REMOVE THIS FOR REAL EXPERIMENT
//		MySQLDataTracker.createSchema(dataModule.getConfiguration());
		
		ts.runExperiment(
				new TestModule(),
				ConfigModules.MYSQL_DATABASE,
				ConfigModules.PERSIST_LOGGING,
				ConfigModules.GROUP_EXPERIMENTS,
				ConfigModules.CREATE_HITS
				);

		Thread.sleep(1000);
		
		// generate fake workers
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
