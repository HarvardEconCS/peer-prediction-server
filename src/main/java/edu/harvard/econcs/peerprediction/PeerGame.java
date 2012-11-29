package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

public abstract class PeerGame<P extends PeerPlayer> {

	int nRounds;
	List<P> players;

	PeerPrior prior;

	PaymentRule paymentRule;

	AtomicInteger currentRoundNum;
	AtomicReference<PeerRound<P>> currentRound;

	ExperimentLog expLog;
	List<PeerResult> results;
	
	public void init(int nRounds2, PeerPrior prior, PaymentRule rule, ExperimentLog expLog) {
		this.nRounds = nRounds2;
		this.prior = prior;
		this.paymentRule = rule;
		this.expLog = expLog;
		
		currentRound = new AtomicReference<PeerRound<P>>(null);
		currentRoundNum = new AtomicInteger();

		results = new ArrayList<PeerResult>();

	}

	public void setPlayers(List<P> players) {
		this.players = players;
	}

	public void startGame() {

		String[] playerNames = new String[players.size()];
		for (int i = 0; i < playerNames.length; i++) {
			playerNames[i] = players.get(i).name;
		}
		double[] paymentArray = paymentRule.getPaymentArray();

		for (P p : players) {
			p.sendGeneralInfo(players.size(), nRounds, playerNames, p.name,
					paymentArray);
		}

		currentRoundNum.set(1);
		startRound();
	}

	public void startRound() {

		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		PeerRound<P> r = new PeerRound<P>(this, chosenWorld, paymentRule, expLog);
		currentRound.set(r);

		r.startRound();
		expLog.printf("Game:\t started round %d", currentRoundNum.get());
	}
	
	public void roundCompleted() {

		expLog.printf("Game:\t round %d completed", currentRoundNum.get());
		
		if (!currentRound.get().isCompleted()) {
			expLog.printf("Error: trying to start next round before current one is completed");
			return;
		}

		// store the results
		this.results.add(currentRound.get().getResult());
		
		if (currentRoundNum.incrementAndGet() > nRounds) {

			// Send players to debrief
			expLog.printf("\nGame:\t All games are finished");
			finishGame();

		} else 
			startRound();

	}

	public abstract void finishGame();
	
	public void reportReceived(P reporter, String report) {
		currentRound.get().reportReceived(reporter, report);
	}

}
