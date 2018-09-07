package org.biclas.station.ws.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.biclas.station.ws.BadInit_Exception;
import org.biclas.station.ws.NoBinaAvail_Exception;
import org.biclas.station.ws.StationView;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;

/**
 * class that tests Bina retrieval
 */
public class GetBiclaIT extends BaseIT {
	private final static int X = 5;
	private final static int Y = 5;
	private final static int CAPACITY = 20;
	private final static int RETURN_PRIZE = 0;
	// static members

	// one-time initialization and clean-up
	@BeforeClass
	public static void oneTimeSetUp() {
		
	}

	@AfterClass
	public static void oneTimeTearDown() {
	}

	// members

	// initialization and clean-up for each test
	@Before
	public void setUp() throws BadInit_Exception {
		client.testClear();
		client.testInit(X, Y, CAPACITY, RETURN_PRIZE);
	}

	@After
	public void tearDown() {
	}

	// main tests
	// assertEquals(expected, actual);

	/** Try to get a Bina , get one verify, one rented (less). */
	@Test
	public void getBinaOneTest() throws NoBiclaAvail_Exception, BadInit_Exception {
		client.getBicla();

		StationView view = client.getInfo();
		assertNotNull(view);
		assertEquals(CAPACITY - 1, view.getAvailableBinas());
	}
	
	@Test
	public void getBinaAllTest() throws NoBiclaAvail_Exception, BadInit_Exception {
		for(int i = 0; i < CAPACITY; i++)
			client.getBicla();

		StationView view = client.getInfo();
		assertNotNull(view);
		assertEquals(0, view.getAvailableBiclas());
	}
	
	/** Try to get a Bina but no Binas available. */
	@Test(expected = NoBiclaAvail_Exception.class)
	public void getBinaNoBinaTest() throws NoBiclaAvail_Exception, BadInit_Exception {
		for(int i = 0; i <= CAPACITY; i++)
			client.getBicla();
	}

	

}
