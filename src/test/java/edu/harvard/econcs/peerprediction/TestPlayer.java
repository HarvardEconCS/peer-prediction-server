package edu.harvard.econcs.peerprediction;

import java.util.Arrays;
import java.util.Random;
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
	 
	private Random rnd;

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
		System.out.printf("%s: Received for display purposes: " +
				"# of rounds %d, # of players %d, paymentArray %s\n", 
				this.name, nRounds, nPlayers, Arrays.toString(paymentArray));
	}

	@Override
	public void sendSignal(String selected) throws WrongStateException {
		if (state != RoundState.NOT_STARTED && state != RoundState.GOT_RESULTS)
			throw new WrongStateException(RoundState.GOT_RESULTS);

		lastSignal.add(selected);
		System.out.printf("%s got signal %s\n", this.name, selected);
	}

	@Override
	public void sendReportConfirmation(PeerPlayer reporter) {
		
		System.out.printf("Player %s's report was received by the server", reporter.name);
		lastReporter.add(reporter);
	}

	@Override
	public void sendResults(String results) {
		System.out.printf("Results received %s", results);
		lastResult.add(results);
	}

	@Override
	public void run() {
		
		rnd = new Random();

		// Repeat until game is finished:
		while (true) {

			// Wait for round signal
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
			this.game.reportReceived(this, localLastReport);

			// Wait for confirmation of report
			try {
				PeerPlayer reporter = lastReporter.take();
				while (!reporter.name.equals(this.name)) {
					reporter = lastReporter.take();
				}
				// TODO:  if the received reporter is not me, 
				// update my count of other players' report status
				state = RoundState.CONFIRMED_REPORT;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}
			
			// Wait for results
			try {
				String results = lastResult.take();
				System.out.printf("Results received: %s", results);
				state = RoundState.GOT_RESULTS;
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

		}

	}

	public class WrongStateException extends RuntimeException {
		public WrongStateException(RoundState expected) {
			super("Expected to be in state: " + expected);
		}
	}
}
