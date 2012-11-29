package edu.harvard.econcs.peerprediction;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestPlayer extends PeerPlayer implements Runnable {

	static enum RoundState {
		SENT_REPORT, CONFIRMED_REPORT, GOT_RESULTS
	};

	volatile RoundState state;

	PeerGame<TestPlayer> game;

	private BlockingQueue<String> lastSignal;
	private String localLastReport;
	private BlockingQueue<PeerPlayer> lastReporter;
	private BlockingQueue<Map<String, Map<String, String>>> lastResult;

	private int otherStatus;
	private int nPlayers;
	private int nRounds;
	private Random rnd;

	/**
	 * 
	 * @param game
	 * @param name
	 */
	public TestPlayer(PeerGame<TestPlayer> game, String name) {
		this.game = game;

		lastSignal = new LinkedBlockingQueue<String>();
		lastReporter = new LinkedBlockingQueue<PeerPlayer>();
		lastResult = new LinkedBlockingQueue<Map<String, Map<String, String>>>();

		this.name = name;
		this.state = RoundState.GOT_RESULTS;
	}

	public String toString() {
		return name;
	}

	@Override
	public void sendGeneralInfo(int nPlayers, int nRounds,
			String[] playerNames, String yourName, double[] paymentArray) {

		this.nPlayers = nPlayers;
		this.nRounds = nRounds;

		game.expLog.printf("%s: Received for display purposes: "
				+ "# of rounds %d, # of players %d, paymentArray %s",
				this.name, nRounds, nPlayers, Arrays.toString(paymentArray));
	}

	@Override
	public void sendSignal(String selectedSignal) throws WrongStateException {

		// System.out.printf("sendSignal called: %s (%s)\n", this.name,
		// this.state);

		lastSignal.add(selectedSignal);
	}

	@Override
	public void sendReportConfirmation(PeerPlayer reporter) {

		// System.out.printf("sendReportConfirmation called: %s (%s)\n",
		// this.name, this.state);

		if (reporter.name.equals(this.name)) {
			if (state != RoundState.SENT_REPORT)
				throw new WrongStateException(this.name, state,
						RoundState.SENT_REPORT + "");
		}

		lastReporter.add(reporter);
	}

	@Override
	public void sendResults(Map<String, Map<String, String>> results) {

		// System.out.printf("sendResults called: %s (%s)\n", this.name,
		// this.state);

		if (state == RoundState.GOT_RESULTS)
			throw new WrongStateException(this.name, state,
					RoundState.CONFIRMED_REPORT + " or "
							+ RoundState.SENT_REPORT);

		lastResult.add(results);
	}

	@Override
	public void run() {

		rnd = new Random();

		int numPlayed = 0;

		// Repeat until game is finished:
		while (true) {

			// Wait for round signal
			this.otherStatus = 0;
			String signal = null;
			try {
				signal = lastSignal.take();

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Pretend that the player is deciding what to report
			// Wait a random amount of time
			try {
				Thread.sleep(rnd.nextInt(2000));
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			// Send report
			state = RoundState.SENT_REPORT;
			localLastReport = signal;
			game.expLog.printf("%s:\t chosen report %s\n", 
					this.name, localLastReport);
			this.game.reportReceived(this, localLastReport);

			int count = 0;
			while (count < nPlayers) {
				try {
					PeerPlayer reporter = lastReporter.take();
					count++;
					if (reporter.name.equals(this.name)) {
						state = RoundState.CONFIRMED_REPORT;
					}
					game.expLog.printf(
							"%s:\t server confirmed report by %s",
							this.name, reporter.name);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Wait for results
			try {
				Map<String, Map<String, String>> result = lastResult.take();
				state = RoundState.GOT_RESULTS;
				game.expLog.printf("%s:\t received results (%s)",
						this.name,  result);
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			numPlayed++;
			if (numPlayed == this.nRounds)
				break;

		}

	}

	public class WrongStateException extends RuntimeException {
		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;

		public WrongStateException(String playerName, RoundState curr,
				String expected) {
			super(playerName + " is in state" + curr
					+ " expected to be in state: " + expected);
		}
	}
}
