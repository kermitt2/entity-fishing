package com.scienceminer.nerd.exceptions;

public class NerdSystemException extends RuntimeException {
    public NerdSystemException() {
        super();
    }

    public NerdSystemException(String message) {
        super(message);
    }

    public NerdSystemException(Throwable cause) {
        super(cause);
    }

    public NerdSystemException(String message, Throwable cause) {
        super(message, cause);
    }
}
