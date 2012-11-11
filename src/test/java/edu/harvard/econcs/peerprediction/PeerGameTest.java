package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import net.andrewmao.math.RandomSelection;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;

import static org.junit.Assert.assertEquals;

public class PeerGameTest {

	@Before
	public void setup() {
		
	}
	
	@After
	public void tearDown() {
		
	}
	
	@Test
	public void test() {
		
		int rounds = 10; // number of rounds
		int nplayers = 3;  // number of players
		
		Map<String, Double> world = PeerPrior.chooseWorld();
		
		// create payment rule
		PaymentRule rule = new PaymentRule();
		rule.addRule("GM", "MM", 0.73);
		rule.addRule("MM", "MM", 0.73);
		rule.addRule("GM", "GM", 0.73);
		rule.addRule("MM", "GM", 0.73);
		//TODO: bug here because pair needs to implement equals and hashcode to be used
		// as key in hashmap
		assertEquals(rule.getPayment("GM", "MM"), 0.73, 0.0000000001);
		
		
		PeerGame game = new PeerGame(rounds, world, rule);
		
		List<TestPlayer> players = new ArrayList<TestPlayer>(nplayers);
		for(int i = 0; i < nplayers; i++) players.add(new TestPlayer(game, "Player " + (i+1)));
		
		game.startGame(players);
		for(TestPlayer p: players) {
			p.run();
		}
		
		
	}

}
