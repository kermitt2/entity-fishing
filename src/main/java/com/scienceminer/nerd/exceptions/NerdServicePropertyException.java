package com.scienceminer.nerd.exceptions;

/**
 * 
 * @author Patrice
 *
 */

public class NerdServicePropertyException extends NerdServiceException
{
	public NerdServicePropertyException() {
		super();
	}
	
	public NerdServicePropertyException(String msg) {
		super(msg);
	}
	
	public NerdServicePropertyException(Throwable cause) {
        super(cause);
    }
	
	public NerdServicePropertyException(String msg, Throwable cause) {
		super(msg, cause);
	}
}
