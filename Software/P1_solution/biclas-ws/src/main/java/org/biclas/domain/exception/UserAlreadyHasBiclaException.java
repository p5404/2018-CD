package org.biclas.domain.exception;

public class UserAlreadyHasBiclaException extends Exception {
	private static final long serialVersionUID = 1L;

	public UserAlreadyHasBiclaException() {
	}

	public UserAlreadyHasBiclaException(String message) {
		super(message);
	}
}
