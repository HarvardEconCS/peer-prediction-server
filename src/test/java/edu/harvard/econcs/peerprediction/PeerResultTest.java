package edu.harvard.econcs.peerprediction;

import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PeerResultTest {

	List<TestPlayer> players;
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
		TestPeerGame game = new TestPeerGame();
		game.init(nRounds, prior, rule);

		// create the players
		players = new ArrayList<TestPlayer>(nplayers);
		for (int i = 0; i < nplayers; i++)
			players.add(new TestPlayer(game, "Player " + (i + 1)));
		
		// Pretend we injected the set of tplayers
		game.setPlayers(players);

		Map<String, Double> chosenWorld = prior.chooseWorld();
		result = new PeerResult(chosenWorld);

	}

	@After
	public void tearDown() throws Exception {
	}

	@Test
	public void test() {
		
		String[] signals = new String[]{"MM", "MM", "GM"};
		for (int i = 0; i < nplayers; i++) {
			result.saveSignal(players.get(i), signals[i]);
			assertTrue(result.resultObject.get(players.get(i).name).get("signal").equals(signals[i]));
		}
		
		String[] reports = new String[]{"GM", "MM", "MM"};
		for (int i = 0; i < nplayers; i++) {
			result.saveReport(players.get(i), reports[i]);
			assertTrue(result.resultObject.get(players.get(i).name).get("report").equals(reports[i]));
		}

		int[] refPlayerIndex = new int[]{2,2,0};

		
	}

}
