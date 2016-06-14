package com.scienceminer.nerd.exceptions;

/**
 * 
 * @author Patrice
 *
 */

public class NerdServiceException extends NerdException
{
	public NerdServiceException() {
		super();
	}
	
	public NerdServiceException(String msg) {
		super(msg);
	}
	
	public NerdServiceException(Throwable cause) {
        super(cause);
    }
	
	public NerdServiceException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
