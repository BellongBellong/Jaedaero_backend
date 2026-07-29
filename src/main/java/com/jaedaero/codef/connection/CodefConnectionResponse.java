package com.jaedaero.codef.connection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "금융기관 연결 및 계좌 동기화 결과")
public class CodefConnectionResponse {
    @ApiModelProperty(value = "사용자 ID", example = "1")
    private final long userId;
    @ApiModelProperty(value = "연결을 요청한 CODEF 금융기관 코드", example = "0238")
    private final String organizationCode;
    @ApiModelProperty(value = "연결 상태", example = "CONNECTED")
    private final String status;
    @ApiModelProperty(value = "이번 요청에서 동기화한 계좌 수", example = "2")
    private final int syncedAccountCount;
    @ApiModelProperty(value = "CODEF 등록 성공 목록")
    private final List<CodefAccountRegistrationResult> successList;
    @ApiModelProperty(value = "CODEF 등록 실패 목록")
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

    public static CodefConnectionResponse alreadyConnected(
            long userId, String organizationCode, int syncedAccountCount) {
        return new CodefConnectionResponse(userId, organizationCode, syncedAccountCount, List.of(), List.of());
    }
    public long getUserId() { return userId; }
    public String getOrganizationCode() { return organizationCode; }
    public String getStatus() { return status; }
    public int getSyncedAccountCount() { return syncedAccountCount; }
    public List<CodefAccountRegistrationResult> getSuccessList() { return successList; }
    public List<CodefAccountRegistrationResult> getErrorList() { return errorList; }
}
