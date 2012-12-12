package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.assertTrue;

import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.api.HITWorkerGroup;
import edu.harvard.econcs.turkserver.server.FakeExperimentController;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

public class PeerResultTest {

	FakeHITWorkerGroup<TestPlayer> players;
	int nplayers;
	PeerResult result;
	PeerPrior prior;
	
	@Before
	public void setUp() throws Exception {
		
		int nRounds = 3; // number of rounds
		nplayers = 3;  // number of players

		// create prior
		prior = PeerPrior.getTestPrior();
		
		// create payment rule
		PaymentRule rule = new PaymentRule();
		rule.addRule("MM", "MM", 0.58);
		rule.addRule("MM", "GM", 0.36);
		rule.addRule("GM", "MM", 0.43);
		rule.addRule("GM", "GM", 0.54);

		// create the game
		ExperimentLog fakeLog = TestUtils.getFakeLog();
		players = TestUtils.getFakeGroup(nplayers, TestPlayer.class);
		FakeExperimentController fakeCont = TestUtils.getFakeController(players);
		
		PeerGame game = new PeerGame(players, fakeLog, fakeCont);
		
		game.init(nRounds, prior, rule);

		Map<String, Double> chosenWorld = prior.chooseWorld();
		result = new PeerResult(chosenWorld);

		fakeCont.setBean(game);
		// start the game
		fakeCont.startExperiment();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		String[] signals = new String[]{"MM", "MM", "GM"};
		
		int i = 0;
		for (HITWorker p : players.getHITWorkers()) {
			result.saveSignal(p, signals[i]);
			assertTrue(result.resultObject.get(p.getHitId()).get("signal").equals(signals[i]));
			i++;
		}
		
		String[] reports = new String[]{"GM", "MM", "MM"};
		i = 0;
		for (HITWorker p : players.getHITWorkers()) {
			result.saveReport(p, reports[i]);
			assertTrue(result.resultObject.get(p.getHitId()).get("report").equals(reports[i]));
			i++;
		}

		int[] refPlayerIndex = new int[]{2,2,0};
		
	}

}
