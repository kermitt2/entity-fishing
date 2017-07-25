package com.scienceminer.nerd.exceptions;

/**
 * This exception is used to communicate problems in the input query at validation time.
 * Should be converted to a 400 http error code
 */
public class QueryException extends RuntimeException {

    public QueryException() {
        super();
    }

    public QueryException(String message) {
        super(message);
    }

    public QueryException(Throwable cause) {
        super(cause);
    }

    public QueryException(String message, Throwable cause) {
        super(message, cause);
    }
}
