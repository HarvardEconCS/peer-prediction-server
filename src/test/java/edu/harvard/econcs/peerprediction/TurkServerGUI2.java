package edu.harvard.econcs.peerprediction;

import java.io.FileNotFoundException;

import org.apache.commons.configuration.ConfigurationException;

import edu.harvard.econcs.turkserver.config.DataModule;
import edu.harvard.econcs.turkserver.server.TurkServer;

public class TurkServerGUI2 {

	/**
	 * @param args
	 * @throws ConfigurationException 
	 * @throws FileNotFoundException 
	 */
	public static void main(String[] args) throws Exception {

		DataModule dm = new DataModule("testing.properties");
		
		new TurkServer(dm);
		
	}

}
