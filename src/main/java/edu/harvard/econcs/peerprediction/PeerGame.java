package edu.harvard.econcs.peerprediction;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;

public abstract class PeerGame<P extends PeerPlayer> {

	int nRounds;
	List<P> players;

	PeerPrior prior;

	PaymentRule paymentRule;

	AtomicInteger currentRoundNum;
	AtomicReference<PeerRound<P>> currentRound;

	List<PeerResult> results;

	public PeerGame(int nRounds2, PeerPrior prior, PaymentRule rule) {

		this.nRounds = nRounds2;
		this.prior = prior;
		this.paymentRule = rule;

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

		// set current round number and create current round
		currentRoundNum.set(1);

		Map<String, Double> chosenWorld = this.prior.chooseWorld();
		PeerRound<P> r = new PeerRound<P>(this, chosenWorld, paymentRule);
		currentRound.set(r);

		// start current round
		r.startRound();
		System.out.printf("\n\nGame:\t Started round %d\n",
				currentRoundNum.get());
	}

	public void roundCompleted() {

		if (!currentRound.get().isCompleted()) {
			System.out
					.println("Error: trying to start next round before current one is completed");
			return;
		}

		// store the results
		this.results.add(currentRound.get().getResult());

		if (currentRoundNum.incrementAndGet() > nRounds) {

			// Send players to debrief
			System.out.println("\nGame:\t All games are finished");

		} else {

			// create a new round
			Map<String, Double> chosenWorld = this.prior.chooseWorld();
			PeerRound<P> r = new PeerRound<P>(this, chosenWorld, paymentRule);
			currentRound.set(r);

			r.startRound();
			System.out.printf("\n\nGame:\t Started round %d\n",
					currentRoundNum.get());
		}
	}

	public void reportReceived(P reporter, String report) {
		currentRound.get().reportReceived(reporter, report);
	}

}
