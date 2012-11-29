package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.api.ExperimentLog;

public class TestPeerGame extends PeerGame<TestPlayer> {

	public void init(int nRounds2, PeerPrior prior, PaymentRule rule) {
		super.init(nRounds2, prior, rule, new ExperimentLog() {
			@Override
			public void print(String msg) {
				System.out.println(msg);
			}

			@Override
			public void printf(String format, Object... args) {
				System.out.printf(format, args);
				System.out.println();
			}			
		});		
	}

	@Override
	public void finishGame() {
		// TODO Auto-generated method stub		
	}

}
