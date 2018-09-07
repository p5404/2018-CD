package org.biclas.ws.cli;


public class BiclasClientException extends Exception {

	private static final long serialVersionUID = 1L;

	public BiclasClientException() {
    }

    public BiclasClientException(String message) {
        super(message);
    }

    public BiclasClientException(Throwable cause) {
        super(cause);
    }

    public BiclasClientException(String message, Throwable cause) {
        super(message, cause);
    }

}
