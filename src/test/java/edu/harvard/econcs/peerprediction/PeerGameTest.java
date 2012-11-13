package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

public class PeerGameTest {

	@Before
	public void setup() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void test() {
		
		int rounds = 3; // number of rounds
		int nplayers = 3;  // number of players
		
		Map<String, Double> world = PeerPrior.chooseWorld();
		
		// create payment rule
		PaymentRule rule = new PaymentRule();
		rule.addRule("MM", "MM", 0.58);
		rule.addRule("MM", "GM", 0.36);
		rule.addRule("GM", "MM", 0.43);
		rule.addRule("GM", "GM", 0.54);
		
		PeerGame game = new PeerGame(rounds, world, rule);
		
		List<TestPlayer> players = new ArrayList<TestPlayer>(nplayers);
		for (int i = 0; i < nplayers; i++)
			players.add(new TestPlayer(game, "Player " + (i + 1)));

		game.startGame(players);
		List<Thread> threads = new ArrayList<Thread>();
		for(TestPlayer p: players) {
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
