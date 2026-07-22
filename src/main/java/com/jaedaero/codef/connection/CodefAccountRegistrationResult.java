package com.jaedaero.codef.connection;

public class CodefAccountRegistrationResult {

    private final String organization;
    private final String code;
    private final String message;

    public CodefAccountRegistrationResult(String organization, String code, String message) {
        this.organization = organization;
        this.code = code;
        this.message = message;
    }

    public String getOrganization() {
        return organization;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
