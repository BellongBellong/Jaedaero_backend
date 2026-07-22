package com.jaedaero.codef.common;

/** Exception for a failed CODEF resource API call. */
public class CodefApiException extends RuntimeException {

    private final int statusCode;

    public CodefApiException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CodefApiException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
