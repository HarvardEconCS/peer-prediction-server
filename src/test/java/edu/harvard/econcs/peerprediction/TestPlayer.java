package edu.harvard.econcs.peerprediction;

import java.util.Arrays;
import java.util.Random;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

public class TestPlayer extends PeerPlayer implements Runnable {

	static enum RoundState {
		NOT_STARTED, HAVE_SIGNAL, SENT_REPORT, CONFIRMED_REPORT, GOT_RESULTS
	};

	volatile RoundState state;

	private BlockingQueue<String> lastSignal;
	private String localLastReport;
	private BlockingQueue<PeerPlayer> lastReporter;
	private BlockingQueue<String> lastResult;

	private int otherStatus;
	private int nPlayers;
	private int nRounds;
	private Random rnd;

	/**
	 * 
	 * @param game
	 * @param name
	 */
	public TestPlayer(PeerGame game, String name) {
		super(game);

		lastSignal = new LinkedBlockingQueue<String>();
		lastReporter = new LinkedBlockingQueue<PeerPlayer>();
		lastResult = new LinkedBlockingQueue<String>();

		this.name = name;
		this.state = RoundState.NOT_STARTED;
	}

	public String toString() {
		return name;
	}

	@Override
	public void sendGeneralInfo(int nRounds, int nPlayers, double[] paymentArray) {

		this.nPlayers = nPlayers;
		this.nRounds = nRounds;

		System.out.printf("%s: Received for display purposes: "
				+ "# of rounds %d, # of players %d, paymentArray %s\n",
				this.name, nRounds, nPlayers, Arrays.toString(paymentArray));
	}

	@Override
	public void sendSignal(String selectedSignal) throws WrongStateException {

		if (state != RoundState.NOT_STARTED 
				&& state != RoundState.GOT_RESULTS 
				&& state != RoundState.SENT_REPORT 
				&& state != RoundState.CONFIRMED_REPORT)
			throw new WrongStateException(this.name, state, 
					RoundState.GOT_RESULTS 
					+ " or " + RoundState.NOT_STARTED 
					+ " or " + RoundState.SENT_REPORT
					+ " or " + RoundState.CONFIRMED_REPORT);

		lastSignal.add(selectedSignal);
	}

	@Override
	public void sendReportConfirmation(PeerPlayer reporter) {

		if (reporter.name.equals(this.name)) {
			if (state != RoundState.SENT_REPORT)
				throw new WrongStateException(this.name, state, RoundState.SENT_REPORT + "");
		}

		lastReporter.add(reporter);
	}

	@Override
	public void sendResults(String results) {

		 if (state != RoundState.CONFIRMED_REPORT && state != RoundState.SENT_REPORT)
			 throw new WrongStateException(this.name, state, RoundState.CONFIRMED_REPORT + " or " + RoundState.SENT_REPORT);

		lastResult.add(results);
	}

	@Override
	public void run() {

		rnd = new Random();

		// Periodically check report update
		Timer t = new Timer();
		t.scheduleAtFixedRate(new CheckStatusTask(this), 0, 2000);

		int numPlayed = 0;
		
		// Repeat until game is finished:
		while (true) {

			state = RoundState.NOT_STARTED;
			
			// Wait for round signal
			this.otherStatus = 0;
			String signal = null;
			try {
				signal = lastSignal.take();
				state = RoundState.HAVE_SIGNAL;

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
			System.out.printf("%s: chosen report %s\n", this.name,
					localLastReport);
			this.game.reportReceived(this, localLastReport);

			// Wait for results
			try {

				String results = lastResult.take();
				System.out.printf("%s: received results (%s)\n", this.name,
						results);
				state = RoundState.GOT_RESULTS;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			numPlayed++;
			if (numPlayed == this.nRounds)
				break;


		}
		
		System.out.printf("%s: All games are finished\n", this.name);

	}

	/**
	 * 
	 * @author alicexigao
	 * 
	 */
	public class CheckStatusTask extends TimerTask {

		private TestPlayer player;

		public CheckStatusTask(TestPlayer p) {
			this.player = p;
		}

		@Override
		public void run() {
			try {
				PeerPlayer reporter = lastReporter.take();
				if (reporter.name.equals(this.player.name)) {
					this.player.state = TestPlayer.RoundState.CONFIRMED_REPORT;
					if (player.otherStatus == player.nPlayers - 1) {
						this.cancel();
					}
				} else {
					player.otherStatus++;
					if (this.player.state == TestPlayer.RoundState.CONFIRMED_REPORT
							&& player.otherStatus == player.nPlayers - 1) {
						this.cancel();
					}
				}

			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	public class WrongStateException extends RuntimeException {
		public WrongStateException(String playerName, RoundState curr, String expected) {
			super(playerName + " is in state" + curr + " expected to be in state: " + expected);
		}
	}
}
