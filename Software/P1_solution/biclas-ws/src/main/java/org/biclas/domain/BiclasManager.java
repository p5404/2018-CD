package org.biclas.domain;

import java.util.ArrayList;
import java.util.Collection;
import java.util.regex.Pattern;

import org.biclas.domain.exception.BadInitException;
import org.biclas.domain.exception.InsufficientCreditsException;
import org.biclas.domain.exception.InvalidEmailException;
import org.biclas.domain.exception.StationNotFoundException;
import org.biclas.domain.exception.UserAlreadyExistsException;
import org.biclas.domain.exception.UserAlreadyHasBinaException;
import org.biclas.domain.exception.UserHasNoBinaException;
import org.biclas.domain.exception.UserNotFoundException;
import org.biclas.station.ws.BadInit_Exception;
import org.biclas.station.ws.NoBinaAvail_Exception;
import org.biclas.station.ws.NoSlotAvail_Exception;
import org.biclas.station.ws.cli.StationClient;
import org.biclas.station.ws.cli.StationClientException;
import org.biclas.ws.StationView;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDIRecord;

/**
 * BiclasManager class 
 * 
 * Class that have the methods used to get/Return Bina, beginning a station, querying all stations, etc.
 *
 */
public class BiclasManager {
	/**
	 * UDDI server URL
	 */
	private String uddiURL = null;

	/**
	 * Station name
	 */
	private String stationTemplateName = null;

	// Singleton -------------------------------------------------------------

	private BiclasManager() {
	}

	/**
	 * SingletonHolder is loaded on the first execution of Singleton.getInstance()
	 * or the first access to SingletonHolder.INSTANCE, not before.
	 */
	private static class SingletonHolder {
		private static final BiclasManager INSTANCE = new BiclasManager();
	}

	public static synchronized BiclasManager getInstance() {
		return SingletonHolder.INSTANCE;
	}

	// Biclas Logic ----------------------------------------------------------

	public User createUser(String email) throws UserAlreadyExistsException, InvalidEmailException {
		return UsersManager.getInstance().RegisterNewUser(email);
	}

	public User getUser(String email) throws UserNotFoundException {
		return UsersManager.getInstance().getUser(email);
	}
	
	public void rentBina(String stationId, String email) throws UserNotFoundException, InsufficientCreditsException, UserAlreadyHasBinaException, StationNotFoundException, NoBinaAvail_Exception {
		User user = getUser(email);
		synchronized (user) {
			//validate user can rent
			user.validateCanRentBina();

			//validate station can rent
			StationClient stationCli = getStation(stationId);
			stationCli.getBina();
			
			//apply rent action to user
			user.effectiveRent();
		}
	}
	
	public void returnBina(String stationId, String email) throws UserNotFoundException, NoSlotAvail_Exception, UserHasNoBinaException, StationNotFoundException {
		User user = getUser(email);
		synchronized (user) {
			//validate user can rent
			user.validateCanReturnBina();
			
			//validate station can rent
			StationClient stationCli = getStation(stationId);
			int prize = stationCli.returnBina();
			
			//apply rent action to user
			user.effectiveReturn(prize);
		}		
	}

	public StationClient getStation(String stationId) throws StationNotFoundException {

		Collection<String> stations = this.getStations();
		String uddiUrl = BiclasManager.getInstance().getUddiURL();
		
		for (String s : stations) {
			try {
				StationClient sc = new StationClient(uddiUrl, s);
				org.biclas.station.ws.StationView sv = sc.getInfo();
				String idToCompare = sv.getId();
				if (idToCompare.equals(stationId)) {
					return sc;
				}
			} catch (StationClientException e) {
				continue;
			}
		}
		
		throw new StationNotFoundException();
	}
	
	
	// UDDI ------------------------------------------------------------------

	public void initUddiURL(String uddiURL) {
		setUddiURL(uddiURL);
	}

	public void initStationTemplateName(String stationTemplateName) {
		setStationTemplateName(stationTemplateName);
	}

	public String getUddiURL() {
		return uddiURL;
	}

	private void setUddiURL(String url) {
		uddiURL = url;
	}

	private void setStationTemplateName(String sn) {
		stationTemplateName = sn;
	}

	public String getStationTemplateName() {
		return stationTemplateName;
	}

	/**
	 * Get list of stations for a given query
	 * 
	 * @return List of stations
	 */
	public Collection<String> getStations() {
		Collection<UDDIRecord> records = null;
		Collection<String> stations = new ArrayList<String>();
		try {
			UDDINaming uddi = new UDDINaming(uddiURL);
			records = uddi.listRecords(stationTemplateName + "%");
			for (UDDIRecord u : records)
				stations.add(u.getOrgName());
		} catch (UDDINamingException e) {
		}
		return stations;
	}

	public void reset() {
		UsersManager.getInstance().reset();
	}

	public void init(int userInitialPoints) throws BadInitException {
		if(userInitialPoints < 0) {
			throw new BadInitException();
		}
		UsersManager.getInstance().init(userInitialPoints);
	}

	/**
	 * 
	 * Inits a Station with a determined ID, coordinates, capacity and returnPrize
	 * 
	 * @param stationId
	 * @param x
	 * @param y
	 * @param capacity
	 * @param returnPrize
	 * @throws BadInitException
	 * @throws StationNotFoundException
	 */
	public void testInitStation(String stationId, int x, int y, int capacity, int returnPrize) throws BadInitException, StationNotFoundException {
		//validate station can rent
		StationClient stationCli;
		try {
			stationCli = getStation(stationId);
			stationCli.testInit(x, y, capacity, returnPrize);
		} catch (BadInit_Exception e) {
			throw new BadInitException(e.getMessage());
		}
		
	}
}
