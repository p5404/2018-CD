package org.biclas.ws;

import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

import org.binas.domain.BiclasManager;

/**
 * BiclasApp
 * 
 * Class that registers itself in UDDI and waits for connections
 * Addresses are passed via pom.xml
 *
 */
public class BiclasApp {

	public static void main(String[] args) throws Exception {
		// Check arguments
		if (args.length == 0 || args.length == 2) {
			System.err.println("Argument(s) missing!");
			System.err.println("Usage: java " + BiclasApp.class.getName() + " wsURL OR uddiURL wsName wsURL");
			return;
		}
		String uddiURL = null;
		String wsName = null;
		String wsURL = null;
		String stationTemplateName = null;

		// Create server implementation object, according to options
		BiclasEndpointManager endpoint = null;
		if (args.length == 1) {
			wsURL = args[0];
			endpoint = new BiclasEndpointManager(wsURL);
		} else if (args.length >= 3) {
			uddiURL = args[0];
			wsName = args[1];
			wsURL = args[2];
			endpoint = new BiclasEndpointManager(uddiURL, wsName, wsURL);
			endpoint.setVerbose(true);
		}
		
		BiclasManager.getInstance().initUddiURL(uddiURL);
		
        // load binas properties
        try {
            InputStream inputStream = BiclasApp.class.getResourceAsStream("/binas.properties");

            Properties properties = new Properties();
            properties.load(inputStream);

			System.out.println("Loaded properties:");
			System.out.println(properties);
            
            stationTemplateName =  properties.getProperty("station.ws.name");
            
            BiclasManager.getInstance().initStationTemplateName(stationTemplateName);

        } catch (IOException e) {
            System.out.printf("Failed to load configuration: %s%n", e);
        }
			

		try {
			endpoint.start();
			endpoint.awaitConnections();
		} finally {
			endpoint.stop();
		}

	}

}