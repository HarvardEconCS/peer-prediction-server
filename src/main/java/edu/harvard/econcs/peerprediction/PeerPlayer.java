package edu.harvard.econcs.peerprediction;

import java.util.Map;

import edu.harvard.econcs.peerprediction.TestPlayer.WrongStateException;

abstract class PeerPlayer {

	protected String name;

	public abstract void sendGeneralInfo(int nRounds, int nPlayers,
			String[] playerNames, String yourName, double[] paymentArray);

	public abstract void sendSignal(String selected) throws WrongStateException;

	public abstract void sendReportConfirmation(PeerPlayer reporter);

	public abstract void sendResults(
			Map<String, Map<String, String>> resultForPlayer);

	
	@Override
	public boolean equals(Object other) {
		if (other instanceof PeerPlayer) {
			return this.name.equals(((PeerPlayer) other).name);
		} else {
			return false;
		}
	}

	@Override
	public int hashCode() {
		return this.name.hashCode();
	}

}