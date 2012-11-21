package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;

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
	public void testPrior() {
		
	}
	
	@Test
	public void testPaymentComputation() {
		
	}
	
	@Test
	public void testRunningTheGame() {
		
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
		PeerGame<TestPlayer> game = new TestPeerGame(nRounds, prior, rule);

		// create the players
		List<TestPlayer> players = new ArrayList<TestPlayer>(nplayers);
		for (int i = 0; i < nplayers; i++)
			players.add(new TestPlayer(game, "Player " + (i + 1)));
		
		// Pretend we injected the set of players
		game.setPlayers(players);
		
		// start the game
		game.startGame();
		
		// create the player threads and start them
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
