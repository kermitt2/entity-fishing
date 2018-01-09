package com.scienceminer.nerd.exceptions;

/**
 * This exception is used to communicate problems in the input query at validation time.
 * Should be converted to a 400 http error code
 */
public class QueryException extends RuntimeException {
    public static String LANGUAGE_ISSUE = "languageIssueReason";
    public static String QUERY_GENERIC_ISSUE = "genericIssue";
    public static String FILE_ISSUE = "fileIssue";

    private String reason = QUERY_GENERIC_ISSUE;

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


    public QueryException(String message, String reason) {
        super(message);
        this.reason = reason;
    }

    public QueryException(Throwable cause, String reason) {
        super(cause);
        this.reason = reason;
    }

    public QueryException(String message, Throwable cause, String reason) {
        super(message, cause);
        this.reason = reason;
    }

    public String getReason() {
        return reason;
    }
}
