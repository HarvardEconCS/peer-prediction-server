package edu.harvard.econcs.peerprediction;

import java.util.List;

import org.eclipse.jetty.util.resource.Resource;

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

public class ServerTestRecap {

	static final String configFile = "testing.properties";
	
	static final int groupSize = 2;
	static final int nRounds   = 4;
	
	static final int fakeWorkers = 0;
	
	static final double passRate = 1;
	static final int maxTries 	 = 1;
	
	static final String setId = "mar-13-fixed";
	
	static class TestModule extends ServerModule {
		
		@Override
		public void configure() {
			super.configure();
			
			bind(QuizFactory.class).toInstance(new QuizFactory.StringQuizFactory("recap"));
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
			
			// Add fake players
			bindConfigurator(new ConfiguratorWithFakePlayers(nRounds, groupSize));	

			// Set ID
			bindString(TSConfig.EXP_SETID, setId);
			
			// Qualification
			bind(QualificationRequirement[].class).toInstance(new QualificationRequirement[] {
					QualMaker.getLocaleQual("US"),
					QualMaker.getMinApprovalRateQual(95)
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
