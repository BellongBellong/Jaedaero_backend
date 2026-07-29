package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** Registration state of one securities institution persisted for a user. */
@ApiModel(description = "사용자별 증권사 등록 상태")
public class SecuritiesRegistrationStatusResponse {
    @ApiModelProperty(value = "사용자 ID", example = "1")
    private final long userId;
    @ApiModelProperty(value = "CODEF 증권사 기관 코드", example = "0238")
    private final String organizationCode;
    @ApiModelProperty(value = "증권사 표시명", example = "미래에셋증권")
    private final String institutionName;
    @ApiModelProperty(value = "사용자에게 Connected ID가 발급되어 DB에 저장됐는지 여부", example = "true")
    private final boolean connectedIdIssued;
    @ApiModelProperty(value = "해당 증권사가 CODEF Connected ID에 정상 등록됐는지 여부", example = "false")
    private final boolean registered;
    @ApiModelProperty(value = "DB에 저장된 해당 증권사 계좌 수", example = "0")
    private final int linkedAccountCount;

    public SecuritiesRegistrationStatusResponse(
            long userId,
            String organizationCode,
            String institutionName,
            boolean connectedIdIssued,
            boolean registered,
            int linkedAccountCount) {
        this.userId = userId;
        this.organizationCode = organizationCode;
        this.institutionName = institutionName;
        this.connectedIdIssued = connectedIdIssued;
        this.registered = registered;
        this.linkedAccountCount = linkedAccountCount;
    }

    public long getUserId() { return userId; }
    public String getOrganizationCode() { return organizationCode; }
    public String getInstitutionName() { return institutionName; }
    public boolean isConnectedIdIssued() { return connectedIdIssued; }
    public boolean isRegistered() { return registered; }
    public int getLinkedAccountCount() { return linkedAccountCount; }
}
