package com.jaedaero.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** Development-only request for KB Kookmin Bank demand-deposit transaction history. */
@ApiModel(description = "KB국민은행 입출금계좌 거래내역 조회 요청")
public class CodefKookminTransactionListTestRequest {

    @NotBlank
    @ApiModelProperty(value = "CODEF Connected ID", required = true)
    private String connectedId;

    @NotBlank
    @ApiModelProperty(value = "보유계좌 조회 응답의 계좌번호(resAccount)", required = true)
    private String account;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "시작일은 yyyyMMdd 형식이어야 합니다.")
    @ApiModelProperty(value = "조회 시작일(yyyyMMdd)", required = true, example = "20260701")
    private String startDate;

    @NotBlank
    @Pattern(regexp = "\\d{8}", message = "종료일은 yyyyMMdd 형식이어야 합니다.")
    @ApiModelProperty(value = "조회 종료일(yyyyMMdd)", required = true, example = "20260722")
    private String endDate;

    @ApiModelProperty(value = "계좌 비밀번호. 기관이 요구하는 경우에만 입력하며 저장·로그 출력하지 않습니다.")
    private String accountPassword;

    public String getConnectedId() {
        return connectedId;
    }

    public void setConnectedId(String connectedId) {
        this.connectedId = connectedId;
    }

    public String getAccount() {
        return account;
    }

    public void setAccount(String account) {
        this.account = account;
    }

    public String getStartDate() {
        return startDate;
    }

    public void setStartDate(String startDate) {
        this.startDate = startDate;
    }

    public String getEndDate() {
        return endDate;
    }

    public void setEndDate(String endDate) {
        this.endDate = endDate;
    }

    public String getAccountPassword() {
        return accountPassword;
    }

    public void setAccountPassword(String accountPassword) {
        this.accountPassword = accountPassword;
    }
}
