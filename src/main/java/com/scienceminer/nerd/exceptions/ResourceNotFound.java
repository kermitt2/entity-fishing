package com.scienceminer.nerd.exceptions;

/**
 * This exception is used to communicate problems when the resource is not found.
 * Should be converted to a 404 http error code
 */

public class ResourceNotFound extends RuntimeException {

    public static final String RESOURCE_ISSUE = "resourceIssue";

    public String reason = RESOURCE_ISSUE;

    public ResourceNotFound() {super();}

    public ResourceNotFound(String message) {
        super(message);
    }

    public ResourceNotFound(Throwable cause) {
        super(cause);
    }

    public ResourceNotFound(String message, Throwable cause) {
        super(message, cause);
    }

    public ResourceNotFound(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public ResourceNotFound(Throwable cause, String reason) {
        super(cause);
        this.reason = reason;
    }

    public ResourceNotFound(String message, Throwable cause, String reason) {
        super(message, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }

}
