package com.scienceminer.nerd.exceptions;

/**
 * 
 * @author Patrice
 *
 */

public class NerdResourceException extends NerdException {

    public NerdResourceException() {
        super();
    }

    public NerdResourceException(String message) {
        super(message);
    }

    public NerdResourceException(Throwable cause) {
        super(cause);
    }

    public NerdResourceException(String message, Throwable cause) {
        super(message, cause);
    }
}
