package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.peerprediction.TestPlayer.WrongStateException;

/**
 * The player class
 * @author alicexigao
 *
 */
abstract class PeerPlayer {
	protected PeerGame game;
	protected String name;

	public PeerPlayer(PeerGame game) {
		this.game = game;
	}

	/**
	 * Send general information before rounds start
	 * @param nRounds
	 * @param size
	 * @param paymentArray
	 */
	public abstract void sendGeneralInfo(int nRounds, int size, double[] paymentArray) ;

	/**
	 * Send signal to the player
	 * 
	 * @param selected
	 * @throws WrongStateException
	 */
	public abstract void sendSignal(String selected)
			throws WrongStateException;

	/**
	 * tells this player that reporter has submitted a report (may be
	 * themself)
	 * 
	 * @param reporter
	 */
	public abstract void sendReportConfirmation(PeerPlayer reporter);

	/**
	 * Send the results of the current round to the player
	 * 
	 * @param object
	 */
	public abstract void sendResults(String results);


}