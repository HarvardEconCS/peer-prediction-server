package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.server.FakeExperimentController;
import edu.harvard.econcs.turkserver.server.FakeHITWorker;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

public class PeerGameWithFakeTest {

	@Before
	public void setUp() throws Exception {
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() throws Exception {
		
		int nRounds = 3; // number of rounds
		int nplayers = 3;  // number of players
		int nFakePlayers = 3;

		// create prior
		PeerPrior prior = PeerPrior.getTestPrior();
		
		// create payment rule
		PaymentRule rule = PaymentRule.getTestPaymentRule();

		// create the game
		ExperimentLog fakeLog = TestUtils.getFakeLog();
		FakeHITWorkerGroup fakeRealGroup = TestUtils.getFakeGroup("Real", nplayers, TestPlayer.class);
		FakeExperimentController fakeCont = TestUtils.getFakeController(fakeRealGroup);
		
		PeerGame game = new PeerGame(fakeRealGroup, fakeLog, fakeCont);
		
		FakeHITWorkerGroup fakeFakeGroup = TestUtils.getFakeGroup("Fake", nFakePlayers, TestPlayer.class);
		
		fakeCont.addFakeGroup(fakeFakeGroup);
		
		game.init(nRounds, prior, rule, fakeFakeGroup);
		
		fakeCont.setBean(game);
		// start the game
		fakeCont.startExperiment();
		
		// create the player threads and start them
		List<Thread> threads = new ArrayList<Thread>();
		
		// Run real players
		for( HITWorker p: fakeRealGroup.getHITWorkers()) {
			FakeHITWorker fake = (FakeHITWorker) p;
			Thread curr = new Thread((TestPlayer) fake.getClientBean());
			threads.add(curr);
			curr.start();
		}
		
		// Run fake players
		for( HITWorker p: fakeFakeGroup.getHITWorkers()) {
			FakeHITWorker fake = (FakeHITWorker) p;
			
			TestPlayer player = (TestPlayer) fake.getClientBean();
			player.overrideConfirmationSignal(fake, game);
			
			Thread curr = new Thread(player);
			threads.add(curr);
			curr.start();
		}
		
		for (Thread t: threads) {
			try {
				t.join();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
		}
		
	}

}
