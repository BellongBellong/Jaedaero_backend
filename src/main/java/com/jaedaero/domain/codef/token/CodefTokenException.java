package com.jaedaero.domain.codef.token;

/** Thrown when CODEF OAuth token issuance fails. Sensitive response data is intentionally omitted. */
public class CodefTokenException extends RuntimeException {

    private final int statusCode;

    public CodefTokenException(String message, int statusCode) {
        super(message);
        this.statusCode = statusCode;
    }

    public CodefTokenException(String message, int statusCode, Throwable cause) {
        super(message, cause);
        this.statusCode = statusCode;
    }

    public int getStatusCode() {
        return statusCode;
    }
}
