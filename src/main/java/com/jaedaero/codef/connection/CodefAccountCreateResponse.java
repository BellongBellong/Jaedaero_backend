package com.jaedaero.codef.connection;

import java.util.List;

/** Result of the CODEF account-create call. Keep connectedId inside the server boundary. */
public class CodefAccountCreateResponse {

    private final String connectedId;
    private final List<CodefAccountRegistrationResult> successList;
    private final List<CodefAccountRegistrationResult> errorList;

    public CodefAccountCreateResponse(
            String connectedId,
            List<CodefAccountRegistrationResult> successList,
            List<CodefAccountRegistrationResult> errorList) {
        this.connectedId = connectedId;
        this.successList = successList;
        this.errorList = errorList;
    }

    public String getConnectedId() {
        return connectedId;
    }

    public List<CodefAccountRegistrationResult> getSuccessList() {
        return successList;
    }

    public List<CodefAccountRegistrationResult> getErrorList() {
        return errorList;
    }
}
