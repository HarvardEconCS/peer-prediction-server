package edu.harvard.econcs.peerprediction;

import java.util.Map;
import java.util.Random;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingQueue;

import com.google.common.collect.ImmutableMap;

import edu.harvard.econcs.turkserver.api.ClientController;
import edu.harvard.econcs.turkserver.api.ExperimentClient;
import edu.harvard.econcs.turkserver.api.ServiceMessage;

@ExperimentClient("peer-prediction-test")
public class TestPlayer implements Runnable {

	static enum RoundState {
		SENT_REPORT, CONFIRMED_REPORT, GOT_RESULTS
	};

	volatile RoundState state;
	private BlockingQueue<String> lastSignal;
	private String localLastReport;
	private BlockingQueue<String> lastReporter;
	private BlockingQueue<Map<String, Map<String, String>>> lastResult;

	private int nPlayers;
	private int nRounds;
	private Random rnd;
	private String[] signalList;

	String strategy = "honest";
	
	final ClientController cont;
	
	/**
	 * 
	 * @param game
	 * @param name
	 */
	public TestPlayer(ClientController cont) {
		this.cont = cont;		

		lastSignal = new LinkedBlockingQueue<String>();
		lastReporter = new LinkedBlockingQueue<String>();
		lastResult = new LinkedBlockingQueue<Map<String, Map<String, String>>>();
		
		this.state = RoundState.GOT_RESULTS;
	}	

	/**
	 * Message format:
	 * status: startRound
	 * numPlayers
	 * numRounds
	 * playersNames
	 * yourName
	 * payments
	 * signalList
	 * 
	 * @param msg
	 */
	@ServiceMessage(key="status", value="startRound")
	public void rcvStatusMsg(Map<String, Object> msg) {
		int nPlayers = 	((Number) msg.get("numPlayers")).intValue();
		
		int nRounds = 	((Number) msg.get("numRounds")).intValue();				
		
		Object[] playerNameObjs = (Object[]) msg.get("playerNames");
		String[] playerNames = new String[playerNameObjs.length];
		for( int i = 0; i < playerNameObjs.length; i++ ) playerNames[i] = playerNameObjs[i].toString();
		
		String yourName = (String) msg.get("yourName");				
		
		Object[] paymentArrayDoubles = (Object[]) msg.get("payments");
		double[] paymentArray = new double[paymentArrayDoubles.length];
		for( int i = 0; i < paymentArrayDoubles.length; i++ ) {
			paymentArray[i] = ((Double) paymentArrayDoubles[i]).doubleValue();
		}
		
		Object[] signalObjs = (Object[]) msg.get("signalList");
		String[] signalList = new String[signalObjs.length];
		for (int i = 0; i < signalObjs.length; i++) {
			signalList[i] = signalObjs[i].toString();
		}
		
		this.rcvGeneralInfo(nPlayers, nRounds, playerNames, yourName, paymentArray, signalList);
	}
	
	/**
	 * Message format:
	 * status: signal
	 * signal
	 * 
	 * @param msg
	 */
	@ServiceMessage(key="status", value="signal")
	public void rcvSignalMsg(Map<String, Object> msg) {
		String signal = (String) msg.get("signal");
		this.rcvSignal(signal);
	}
	
	/**
	 * Message format:
	 * status: confirmReport
	 * playerName
	 * 
	 * @param msg
	 */
	@ServiceMessage(key="status", value="confirmReport")
	public void rcvConfirmReportMsg(Map<String, Object> msg) {
		String playerName = (String) msg.get("playerName");
		this.rcvReportConfirmation(playerName);
	}
	  
	/**
	 * Message format:
	 * status: results
	 * result
	 * 
	 * @param msg
	 */
	@ServiceMessage(key="status", value="results")
	public void rcvResultsMsg(Map<String, Object> msg) {
		Map<String, Map<String, String>> results 
		= PeerResult.deserialize(msg.get("result")); 					
		this.rcvResults(results);
	}
	
	@ServiceMessage
	public void rcvPrivate(Map<String, Object> msg) throws Exception {		
		
		if (msg.containsKey("status")) {
			String status = (String) msg.get("status");
			
			if (status.equals("startRound")) {	
			} else if (status.equals("signal")) {
			} else if (status.equals("confirmReport")) {
			} else if (status.equals("results")) { 
			} else {
				throw new Exception("Test player received unrecognized " +
						"message " + msg.toString());
			}	
		} else {
			throw new Exception("Test player expected status " +
					"in the received message, but did not find it." 
					+ msg.toString());
		}
			
	}

	public void rcvGeneralInfo(int nPlayers, int nRounds,
			String[] playerNames, String yourName, double[] paymentArray, String[] signalList) {

		this.nPlayers = nPlayers;
		this.nRounds = nRounds;
		this.signalList = signalList;

		System.out.printf("%s: received general info", cont.getHitId());
//		System.out.printf("%s: Received for display purposes: "
//				+ "# of rounds %d, # of players %d, paymentArray %s, signalList %s",
//				cont.getHitId(), nRounds, nPlayers, 
//				Arrays.toString(paymentArray), Arrays.toString(signalList));
		System.out.println();
	}

	public void rcvSignal(String selectedSignal) throws WrongStateException {

		lastSignal.add(selectedSignal);
		
		System.out.printf("%s: received signal %s", cont.getHitId(), selectedSignal);
		System.out.println();
	}
	
	public void rcvReportConfirmation(String reporter) {
		
		// TODO:  Should there be a wrong state exception or not?
		if (reporter.equals(cont.getHitId())) {
			if (state != RoundState.SENT_REPORT) {
//				throw new WrongStateException(cont.getHitId(), state, RoundState.SENT_REPORT + "");
				System.out.printf("Warning: Received unexpected state %s from %s", state, cont.getHitId());
				System.out.println();
			}
		}

		lastReporter.add(reporter);
		
		System.out.printf("%s: confirmed report from %s", cont.getHitId(), reporter);
		System.out.println();
	}
	
	public void rcvResults(Map<String, Map<String, String>> results) {

		if (state == RoundState.GOT_RESULTS)
			throw new WrongStateException(cont.getHitId(), state,
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

			state = RoundState.SENT_REPORT;
			localLastReport = this.chooseReport(signal, strategy);
			System.out.printf("%s: chose report %s", cont.getHitId(), localLastReport);
			System.out.println();

			// Send report
			cont.sendExperimentService(ImmutableMap.of("report", (Object) localLastReport));			

			int count = 0;
			while (count < nPlayers) {
				try {
					String reporter = lastReporter.take();
					count++;
					if (reporter.equals(cont.getHitId())) {
						state = RoundState.CONFIRMED_REPORT;
					}
				} catch (InterruptedException e) {
					e.printStackTrace();
				}
			}

			// Wait for results
			try {
				Map<String, Map<String, String>> result = lastResult.take();
				state = RoundState.GOT_RESULTS;
				System.out.printf("%s: received results", cont.getHitId());
				System.out.println();
			} catch (InterruptedException e) {
				e.printStackTrace();
			}

			numPlayed++;
			if (numPlayed == this.nRounds)
				break;

		}

	}

	private String chooseReport(String signal, String strategy) {
		if (strategy.equals("honest"))
			return signal;
		else if (strategy.equals("alwayssignal0"))
			return this.signalList[0];
		else if (strategy.equals("alwayssignal1"))
			return this.signalList[1];
		else if (strategy.equals("random")) {
			Random rnd = new Random();
			return signalList[rnd.nextInt(2)];
		}
		else {
			throw new UnrecognizedStrategyException(strategy);
		}
	}

	public class UnrecognizedStrategyException extends RuntimeException {

		/**
		 * 
		 */
		private static final long serialVersionUID = 1L;
		
		public UnrecognizedStrategyException(String strategy) {
			super("Unrecognized strategy: " + strategy);
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
