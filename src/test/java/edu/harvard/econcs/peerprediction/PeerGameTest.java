package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.server.FakeExperimentController;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

public class PeerGameTest {
	
	@Before
	public void setup() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void testPrior() {
		
	}
	
	@Test
	public void testRunningTheGame() throws Exception {
		
		int nRounds = 3; // number of rounds
		int nplayers = 3;  // number of players

		// create prior
		PeerPrior prior = PeerPrior.getTestPrior();
		
		// create payment rule
		PaymentRule rule = new PaymentRule();
		rule.addRule("MM", "MM", 0.58);
		rule.addRule("MM", "GM", 0.36);
		rule.addRule("GM", "MM", 0.43);
		rule.addRule("GM", "GM", 0.54);

		// create the game
		ExperimentLog fakeLog = TestUtils.getFakeLog();
		FakeHITWorkerGroup<TestPlayer> fakeGroup = TestUtils.getFakeGroup(nplayers, TestPlayer.class);
		FakeExperimentController fakeCont = TestUtils.getFakeController(fakeGroup);
		
		PeerGame game = new PeerGame(fakeGroup, fakeLog, fakeCont);
		
		game.init(nRounds, prior, rule);
		
		fakeCont.setBean(game);
		// start the game
		fakeCont.startExperiment();
		
		// create the player threads and start them
		List<Thread> threads = new ArrayList<Thread>();
		for(TestPlayer p: fakeGroup.getClassedHITWorkers()) {
			Thread curr = new Thread(p);
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
