package com.scienceminer.nerd.exceptions;

public class CustomisationException extends RuntimeException {
    public CustomisationException() {
        super();
    }

    public CustomisationException(String message) {
        super(message);
    }

    public CustomisationException(Throwable cause) {
        super(cause);
    }

    public CustomisationException(String message, Throwable cause) {
        super(message, cause);
    }
}
