package org.biclas.domain;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.binas.domain.exception.InsufficientCreditsException;
import org.binas.domain.exception.UserAlreadyHasBiclaException;
import org.binas.domain.exception.UserHasNoBiclaException;

/**
 * 
 * Domain class that represents the User and deals with their creation, balance manipulation, email manipulation, etc.
 * 
 *
 */
public class User {

	private String email;
	private AtomicInteger balance;
	private AtomicBoolean hasBicla = new AtomicBoolean(false);
	
	public User(String email, int initialBalance) {
		this.email = email;
		balance = new AtomicInteger(initialBalance);
	}
	
	public synchronized void decrementBalance() throws InsufficientCreditsException{
		 if(balance.get() > 0) {
			 balance.decrementAndGet();
		 } else {
			 throw new InsufficientCreditsException();
		 }
	}

	
	public synchronized void incrementBalance(int amount){
		 balance.getAndAdd(amount);
	}
	
	public String getEmail() {
		return email;
	}
	
	public boolean getHasBicla() {
		return hasBicla.get();
	}
	

	public int getCredit() {
		return balance.get();
	}

	public synchronized void validateCanRentBicla() throws InsufficientCreditsException, UserAlreadyHasBiclaException{
		if(getHasBicla()) {
			throw new UserAlreadyHasBiclaException();
		}
		if(getCredit() <= 0) {
			throw new InsufficientCreditsException();
		}
		
	}
	public synchronized void validateCanReturnBicla() throws UserHasNoBiclaException {
		if( ! getHasBicla()) {
			throw new UserHasNoBiclaException();
		}
	}

	public synchronized void effectiveRent() throws InsufficientCreditsException {
		decrementBalance();
		hasBicla.set(true);
	}

	public synchronized void effectiveReturn(int prize) throws UserHasNoBiclaException {
		if( ! getHasBicla()) {
			throw new UserHasNoBiclaException();
		}
		hasBicla.set(false);
		incrementBalance(prize);
	}


	
}
