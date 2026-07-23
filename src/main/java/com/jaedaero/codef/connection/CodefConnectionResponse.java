package com.jaedaero.codef.connection;

import java.util.List;

/** Safe response returned to the frontend. Connected IDs never leave the server. */
public class CodefConnectionResponse {
    private final long userId;
    private final String organizationCode;
    private final String status;
    private final int syncedAccountCount;
    private final List<CodefAccountRegistrationResult> successList;
    private final List<CodefAccountRegistrationResult> errorList;

    public CodefConnectionResponse(long userId, String organizationCode, int syncedAccountCount,
            List<CodefAccountRegistrationResult> successList, List<CodefAccountRegistrationResult> errorList) {
        this.userId = userId;
        this.organizationCode = organizationCode;
        this.status = "CONNECTED";
        this.syncedAccountCount = syncedAccountCount;
        this.successList = successList;
        this.errorList = errorList;
    }
    public long getUserId() { return userId; }
    public String getOrganizationCode() { return organizationCode; }
    public String getStatus() { return status; }
    public int getSyncedAccountCount() { return syncedAccountCount; }
    public List<CodefAccountRegistrationResult> getSuccessList() { return successList; }
    public List<CodefAccountRegistrationResult> getErrorList() { return errorList; }
}
