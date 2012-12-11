package edu.harvard.econcs.peerprediction;

import java.util.Arrays;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.turkserver.server.FakeHITWorker;

public class TestPlayer extends FakeHITWorker implements Runnable {

	static enum RoundState {
		SENT_REPORT, CONFIRMED_REPORT, GOT_RESULTS
	};

	volatile RoundState state;

	private BlockingQueue<String> lastSignal;
	private String localLastReport;
	private BlockingQueue<String> lastReporter;
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
	public TestPlayer() {

		lastSignal = new LinkedBlockingQueue<String>();
		lastReporter = new LinkedBlockingQueue<String>();
		lastResult = new LinkedBlockingQueue<Map<String, Map<String, String>>>();
		
		this.state = RoundState.GOT_RESULTS;
	}	
	
	@Override
	public void rcvBroadcast(Object msg) {

	}

	@Override
	public void rcvPrivate(Object msg) {
		Map<String, Object> castedMsg = (Map<String, Object>) msg;
		
		if (castedMsg.containsKey("status")) {
			String status = (String) castedMsg.get("status");
			if (status.equals("startRound")) {
				int nPlayers = (Integer) castedMsg.get("numPlayers");
				int nRounds = (Integer) castedMsg.get("numRounds");
				String[] playerNames = (String[]) castedMsg.get("playerNames");
				String yourName = (String) castedMsg.get("yourName");
				double[] paymentArray = (double[]) castedMsg.get("payments");
				this.rcvGeneralInfo(nPlayers, nRounds, playerNames, yourName, paymentArray);
				
			} else if (status.equals("signal")) {
				String signal = (String) castedMsg.get("signal");
				this.rcvSignal(signal);
				
			} else if (status.equals("confirmReport")) {
				String playerName = (String) castedMsg.get("playerName");
				this.rcvReportConfirmation(playerName);
				
			} else if (status.equals("results")) { 
				Map<String, Map<String, String>> results 
					= (Map<String, Map<String, String>>) castedMsg.get("results");
				this.rcvResults(results);
				
			} else {
				// unrecognized message
			}
				
		} else {
			// error
		}
			
	}

	public void rcvGeneralInfo(int nPlayers, int nRounds,
			String[] playerNames, String yourName, double[] paymentArray) {

		this.nPlayers = nPlayers;
		this.nRounds = nRounds;

		System.out.printf("%s: Received for display purposes: "
				+ "# of rounds %d, # of players %d, paymentArray %s",
				super.getHitId(), nRounds, nPlayers, Arrays.toString(paymentArray));
		System.out.println();
	}

	public void rcvSignal(String selectedSignal) throws WrongStateException {

		lastSignal.add(selectedSignal);
	}
	
	public void rcvReportConfirmation(String reporter) {

		if (reporter.equals(this.hitId)) {
			if (state != RoundState.SENT_REPORT)
				throw new WrongStateException(super.getHitId(), state,
						RoundState.SENT_REPORT + "");
		}

		lastReporter.add(reporter);
	}
	
	public void rcvResults(Map<String, Map<String, String>> results) {

		if (state == RoundState.GOT_RESULTS)
			throw new WrongStateException(super.getHitId(), state,
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
			System.out.printf("%s:\t chosen report %s\n", 
					super.getHitId(), localLastReport);
			
			super.sendPrivate(ImmutableMap.of("report", (Object) localLastReport));			

			int count = 0;
			while (count < nPlayers) {
				try {
					String reporter = lastReporter.take();
					count++;
					if (reporter.equals(super.getHitId())) {
						state = RoundState.CONFIRMED_REPORT;
					}
					System.out.printf(
							"%s:\t server confirmed report by %s\n",
							super.getHitId(), reporter);
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Wait for results
			try {
				Map<String, Map<String, String>> result = lastResult.take();
				state = RoundState.GOT_RESULTS;
				System.out.printf("%s:\t received results (%s)\n",
						super.getHitId(),  result);
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
