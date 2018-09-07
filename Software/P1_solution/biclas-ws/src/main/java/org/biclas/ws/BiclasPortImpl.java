package org.biclas.ws;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

import javax.jws.WebService;

import org.biclas.domain.BiclasManager;
import org.biclas.domain.StationsComparator;
import org.biclas.domain.User;
import org.biclas.domain.UsersManager;
import org.biclas.domain.exception.BadInitException;
import org.biclas.domain.exception.InsufficientCreditsException;
import org.biclas.domain.exception.InvalidEmailException;
import org.biclas.domain.exception.StationNotFoundException;
import org.biclas.domain.exception.UserAlreadyExistsException;
import org.biclas.domain.exception.UserAlreadyHasBiclaException;
import org.biclas.domain.exception.UserHasNoBiclaException;
import org.biclas.domain.exception.UserNotFoundException;
import org.biclas.station.ws.NoSlotAvail_Exception;
import org.biclas.station.ws.cli.StationClient;

import org.biclas.station.ws.cli.StationClientException;

import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINaming;
import pt.ulisboa.tecnico.sdis.ws.uddi.UDDINamingException;

@WebService(
		endpointInterface = "org.biclas.ws.BiclasPortType",
        wsdlLocation = "biclas.wsdl",
        name ="BiclasWebService",
        portName = "BiclasPort",
        targetNamespace="http://ws.biclas.org/",
        serviceName = "BiclasService"
)
public class BiclasPortImpl implements BiclasPortType {
	
	// end point manager
	private BiclasEndpointManager endpointManager;

	public BiclasPortImpl(BiclasEndpointManager endpointManager) {
		this.endpointManager = endpointManager;
	}

	@Override
	public UserView activateUser(String email) throws InvalidEmail_Exception, EmailExists_Exception {
		try {
			User user = BiclasManager.getInstance().createUser(email);
			
			//Create and populate userView
			UserView userView = new UserView();
			userView.setEmail(user.getEmail());
			userView.setCredit(user.getCredit());
			userView.setHasBicla(user.getHasBicla());
			return userView;
		} catch (UserAlreadyExistsException e) {
			throwEmailExists("Email already exists: " + email);
		} catch (InvalidEmailException e) {
			throwInvalidEmail("Invalid email: " + email);
		}
		return null;
	}

	@Override
	public StationView getInfoStation(String stationId) throws InvalidStation_Exception {
		if(stationId == null || stationId.trim().isEmpty())
			throwInvalidStation("Station IDs can not be empty!");
		
		StationClient stationCli;
		try {
			stationCli = BiclasManager.getInstance().getStation(stationId);
			return newStationView(stationCli.getInfo());
		} catch (StationNotFoundException e) {
			throwInvalidStation("No Station found with ID: " + stationId);
			return null;
		}
		
	}

	@Override
	public List<StationView> listStations(Integer numberOfStations, CoordinatesView coordinates) {
		List<StationView> stationViews = new ArrayList<StationView>();
		Collection<String> stations = BiclasManager.getInstance().getStations();
		String uddiUrl = BiclasManager.getInstance().getUddiURL();
		StationClient sc = null;
		org.biclas.station.ws.StationView sv = null;
		
		if(numberOfStations <= 0 || coordinates == null)
			return stationViews;
		
		for (String s : stations) {
			try {
				sc = new StationClient(uddiUrl, s);
				sv = sc.getInfo();
				stationViews.add(newStationView(sv));
			} catch(StationClientException e) {
				continue;
			}
		}
		Collections.sort(stationViews, new StationsComparator(coordinates));
		
		if(numberOfStations > stationViews.size())
			return stationViews;
		else
			return stationViews.subList(0, numberOfStations);
	}

	@Override
	public void rentBicla(String stationId, String email) throws AlreadyHasBicla_Exception, InvalidStation_Exception,
			NoBiclaAvail_Exception, NoCredit_Exception, UserNotExists_Exception {
		
		try {
			BiclasManager.getInstance().rentBicla(stationId,email);
		} catch (UserNotFoundException e) {
			throwUserNotExists("User not found: " + email);
		} catch (InsufficientCreditsException e) {
			throwNoCredit("User has insufficient credits: " + email);
		} catch (UserAlreadyHasBiclaException e) {
			throwAlreadyHasBicla("User already has bina: " + email);
		} catch (StationNotFoundException e) {
			throwInvalidStation("Station not found: " + stationId);
		} catch (org.biclas.station.ws.NoBiclaAvail_Exception e) {
			throwNoBiclaAvail("Station has no Biclas available: " + stationId);
		}
	}


	@Override
	public void returnBicla(String stationId, String email)
			throws FullStation_Exception, InvalidStation_Exception, NoBiclaRented_Exception, UserNotExists_Exception {
		try {
			BiclasManager.getInstance().returnBicla(stationId,email);
		} catch (UserNotFoundException e) {
			throwUserNotExists("User not found: " + email);
		} catch (NoSlotAvail_Exception e) {
			throwFullStation("Station has NO docks available: " + stationId);
		} catch (UserHasNoBiclaException e) {
			throwNoBiclaRented("User has NO bina: " + email);
		} catch (StationNotFoundException e) {
			throwInvalidStation("Station not found: " + stationId);
		}
	}

	@Override
	public int getCredit(String email) throws UserNotExists_Exception {
		try {
			User user = BiclasManager.getInstance().getUser(email);	
			return user.getCredit();
		} catch (UserNotFoundException e) {
			throwUserNotExists("User not found: " + email);
		}
		return 0;
	}
	
	// Auxiliary operations --------------------------------------------------
	
	@Override
	public String testPing(String inputMessage) {
		final String EOL = String.format("%n");
		StringBuilder sb = new StringBuilder();

		sb.append("Hello ");
		if (inputMessage == null || inputMessage.length()==0)
			inputMessage = "friend";
		sb.append(inputMessage);
		sb.append(" from ");
		sb.append(endpointManager.getWsName());
		sb.append("!");
		sb.append(EOL);
		
		Collection<String> stationUrls = null;
		try {
			UDDINaming uddiNaming = endpointManager.getUddiNaming();
			stationUrls = uddiNaming.list(BiclasManager.getInstance().getStationTemplateName() + "%");
			sb.append("Found ");
			sb.append(stationUrls.size());
			sb.append(" stations on UDDI.");
			sb.append(EOL);
		} catch(UDDINamingException e) {
			sb.append("Failed to contact the UDDI server:");
			sb.append(EOL);
			sb.append(e.getMessage());
			sb.append(" (");
			sb.append(e.getClass().getName());
			sb.append(")");
			sb.append(EOL);
			return sb.toString();
		}

		for(String stationUrl : stationUrls) {
			sb.append("Ping result for station at ");
			sb.append(stationUrl);
			sb.append(":");
			sb.append(EOL);
			try {
				StationClient client = new StationClient(stationUrl);
				String supplierPingResult = client.testPing(endpointManager.getWsName());
				sb.append(supplierPingResult);
			} catch(Exception e) {
				sb.append(e.getMessage());
				sb.append(" (");
				sb.append(e.getClass().getName());
				sb.append(")");
			}
			sb.append(EOL);
		}
		
		return sb.toString();
	}

	@Override
	public void testClear() {
		//Reset Biclas
		BiclasManager.getInstance().reset();

		//Reset All Stations
		Collection<String> stations = BiclasManager.getInstance().getStations();
		String uddiUrl = BiclasManager.getInstance().getUddiURL();
		StationClient sc = null;

		for (String s : stations) {
			try {
				sc = new StationClient(uddiUrl, s);
				sc.testClear();
			} catch(StationClientException e) {
				continue;
			}
		}
	}

	@Override
	public void testInitStation(String stationId, int x, int y, int capacity, int returnPrize)
			throws BadInit_Exception {
		
		try {
			BiclasManager.getInstance().testInitStation(stationId,x,y,capacity,returnPrize);
		} catch (BadInitException e) {
			throwBadInit("Bad init values");
		} catch (StationNotFoundException e) {
			throwBadInit("No Station found with ID: " + stationId);
		}
	}

	@Override
	public void testInit(int userInitialPoints) throws BadInit_Exception {
		try {
			BiclasManager.getInstance().init(userInitialPoints);
		} catch (BadInitException e) {
			throwBadInit("Bad init values: " + userInitialPoints);
		}
	}
	
	
	// View helpers ----------------------------------------------------------
	
	private StationView newStationView(org.biclas.station.ws.StationView sv) {
		StationView retSv = new StationView();
		CoordinatesView coordinates = new CoordinatesView();
		coordinates.setX(sv.getCoordinate().getX());
		coordinates.setY(sv.getCoordinate().getY());
		
		retSv.setCapacity(sv.getCapacity());
		retSv.setCoordinate(coordinates);
		retSv.setAvailableBiclas(sv.getAvailableBiclas());
		retSv.setFreeDocks(sv.getFreeDocks());
		retSv.setId(sv.getId());
		retSv.setTotalGets(sv.getTotalGets());
		retSv.setTotalReturns(sv.getTotalReturns());
		return retSv;
	}
	
	// Exception helpers -----------------------------------------------------
	
	private void throwInvalidEmail(final String message) throws InvalidEmail_Exception {
		InvalidEmail faultInfo = new InvalidEmail();
		faultInfo.setMessage(message);
		throw new InvalidEmail_Exception(message, faultInfo);
	}
	
	private void throwEmailExists(final String message) throws EmailExists_Exception {
		EmailExists faultInfo = new EmailExists();
		faultInfo.setMessage(message);
		throw new EmailExists_Exception(message, faultInfo);
	}
	
	private void throwInvalidStation(final String message) throws InvalidStation_Exception {
		InvalidStation faultInfo = new InvalidStation();
		faultInfo.setMessage(message);
		throw new InvalidStation_Exception(message, faultInfo);
	}
	
	private void throwUserNotExists(final String message) throws UserNotExists_Exception {
		UserNotExists faultInfo = new UserNotExists();
		faultInfo.setMessage(message);
		throw new UserNotExists_Exception(message, faultInfo);
	}
	
	private void throwNoCredit(final String message) throws NoCredit_Exception {
		NoCredit faultInfo = new NoCredit();
		faultInfo.setMessage(message);
		throw new NoCredit_Exception(message, faultInfo);
	}
	
	private void throwAlreadyHasBicla(final String message) throws AlreadyHasBicla_Exception {
		AlreadyHasBicla faultInfo = new AlreadyHasBicla();
		faultInfo.setMessage(message);
		throw new AlreadyHasBicla_Exception(message, faultInfo);
	}
	
	private void throwNoBiclaAvail(final String message) throws NoBiclaAvail_Exception {
		NoBiclaAvail faultInfo = new NoBiclaAvail();
		faultInfo.setMessage(message);
		throw new NoBiclaAvail_Exception(message, faultInfo);
	}
	
	private void throwNoBiclaRented(final String message) throws NoBiclaRented_Exception {
		NoBiclaRented faultInfo = new NoBiclaRented();
		faultInfo.setMessage(message);
		throw new NoBiclaRented_Exception(message, faultInfo);
	}
	
	private void throwFullStation(final String message) throws FullStation_Exception {
		FullStation faultInfo = new FullStation();
		faultInfo.setMessage(message);
		throw new FullStation_Exception(message, faultInfo);
	}

	private void throwBadInit(final String message) throws BadInit_Exception {
		BadInit faultInfo = new BadInit();
		faultInfo.setMessage(message);
		throw new BadInit_Exception(message, faultInfo);
	}
}
