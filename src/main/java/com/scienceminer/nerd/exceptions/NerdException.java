package com.scienceminer.nerd.exceptions;

public class NerdException extends RuntimeException {

    public NerdException() {
        super();
    }

    public NerdException(String message) {
        super(message);
    }

    public NerdException(Throwable cause) {
        super(cause);
    }

    public NerdException(String message, Throwable cause) {
        super(message, cause);
    }
}
