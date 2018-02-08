package com.scienceminer.nerd.exceptions;

/**
 * This exception is used to communicate problems when the resource is not found.
 * Should be converted to a 404 http error code
 */

public class ResourceNotFound extends RuntimeException {

    public ResourceNotFound(String message) {
        super(message);
    }
}
