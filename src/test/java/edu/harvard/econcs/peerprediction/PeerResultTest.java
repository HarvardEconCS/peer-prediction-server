package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import edu.harvard.econcs.peerprediction.PaymentRule;
import edu.harvard.econcs.peerprediction.PeerGame;
import edu.harvard.econcs.peerprediction.PeerPrior;
import edu.harvard.econcs.peerprediction.PeerResult;
import edu.harvard.econcs.turkserver.api.ExperimentLog;
import edu.harvard.econcs.turkserver.api.HITWorker;
import edu.harvard.econcs.turkserver.server.FakeExperimentController;
import edu.harvard.econcs.turkserver.server.FakeHITWorkerGroup;
import edu.harvard.econcs.turkserver.server.TestUtils;

public class PeerResultTest {

	double eps = 0.00000000000001;
	
	FakeHITWorkerGroup players;
	int nplayers;
	PeerResult result;
	PeerPrior prior;
	PaymentRule rule;

	@Before
	public void setUp() throws Exception {
		
		int nRounds = 3; // number of rounds
		nplayers = 3;  // number of players

		// create prior
		prior = PeerPrior.getTestPrior();
		
		// create payment rule
		rule = PaymentRule.getTestPaymentRule();

		// create the game
		ExperimentLog fakeLog = TestUtils.getFakeLog();
		players = TestUtils.getFakeGroup(nplayers, TestPlayer.class);
		FakeExperimentController fakeCont = TestUtils.getFakeController(players);
		
		PeerGame game = new PeerGame(players, fakeLog, fakeCont);
		
		game.init(nRounds, prior, rule);

		Map<String, Double> chosenWorld = prior.chooseWorld();
		result = new PeerResult(chosenWorld);

//		fakeCont.setBean(game);
//		fakeCont.startExperiment();
	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		String[] signals = new String[]{"MM", "MM", "GB"};
		String[] reports = new String[]{"GB", "MM", "MM"};
		
		int i = 0;
		for (HITWorker p : players.getHITWorkers()) {
			result.saveSignal(p, signals[i]);
			result.saveReport(p, reports[i]);
			i++;
		}
		
		i = 0;
		for (HITWorker p : players.getHITWorkers()) {
			assertTrue(result.resultObject.get(p.getHitId()).get("signal").equals(signals[i]));
			assertTrue(result.resultObject.get(p.getHitId()).get("report").equals(reports[i]));
			i++;
		}
		
		result.computePayments(rule);
		
		List<String> playerIds = new ArrayList<String>();
		for (HITWorker p : players.getHITWorkers())
			playerIds.add(p.getHitId());
		
		for (HITWorker p : players.getHITWorkers()) {
			String refPlayerId = result.resultObject.get(p.getHitId()).get("refPlayer");
			if (refPlayerId.equals(p.getHitId()))
				fail("Reference player is the same as the current player");
			if (!playerIds.contains(refPlayerId))
				fail("Reference player is not a valid player");
			
			String myReport = result.resultObject.get(p.getHitId()).get("report");
			String otherReport = result.resultObject.get(refPlayerId).get("report");
			double reward = rule.getPayment(myReport, otherReport);
		
			assertEquals(reward, Double.parseDouble(result.resultObject.get(p.getHitId()).get("reward")), eps);
		}
		
		System.out.println(result.toString());
		
	}

}
