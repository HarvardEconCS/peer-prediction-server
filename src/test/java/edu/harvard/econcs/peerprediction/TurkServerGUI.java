package edu.harvard.econcs.peerprediction;

import edu.harvard.econcs.turkserver.server.TurkServer;

public class TurkServerGUI {

	/**
	 * @param args
	 */
	public static void main(String[] args) throws Exception {
		new TurkServer("testing.properties");
	}

}
