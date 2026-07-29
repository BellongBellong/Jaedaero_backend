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

    public boolean isOrganizationRegistered(String organizationCode) {
        return successList.stream().anyMatch(result -> organizationCode.equals(result.getOrganization())
                && "CF-00000".equals(result.getCode()));
    }

    public String organizationErrorMessage(String organizationCode) {
        return errorList.stream()
                .filter(result -> organizationCode.equals(result.getOrganization()))
                .map(CodefAccountRegistrationResult::getMessage)
                .findFirst()
                .orElse("CODEF가 기관 계정 등록을 완료하지 않았습니다.");
    }
}
