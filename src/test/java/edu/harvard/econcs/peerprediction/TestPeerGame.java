package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PaymentRule;
import edu.harvard.econcs.peerprediction.PeerGame;
import edu.harvard.econcs.peerprediction.PeerPrior;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.server.FakeExperimentController;
import edu.harvard.econcs.turkserver.server.FakeHITWorker;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

public class TestPeerGame {
	
	@Before
	public void setup() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void testRunningTheGame() throws Exception {
		
		int nRounds = 6; // number of rounds
		int nplayers = 4;  // number of players

		// create prior
		PeerPrior prior = PeerPrior.getTestPrior();
		
		// create payment rule
		PaymentRule rule = PaymentRule.getTestPaymentRule();

		// create the game
		ExperimentLog fakeLog = TestUtils.getFakeLog();
		FakeHITWorkerGroup fakeGroup = TestUtils.getFakeGroup("", nplayers, TestPlayer.class);
		FakeExperimentController fakeCont = TestUtils.getFakeController(fakeGroup);
		
		PeerGame game = new PeerGame(fakeGroup, fakeLog, fakeCont);
		
		game.init(nRounds, prior, rule);
		
		fakeCont.setBean(game);
		// start the game
		fakeCont.startExperiment();
		
		// create the player threads and start them
		List<Thread> threads = new ArrayList<Thread>();
		for(HITWorker p: fakeGroup.getHITWorkers()) {
			FakeHITWorker fake = (FakeHITWorker) p;
			Thread curr = new Thread((TestPlayer) fake.getClientBean());
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
