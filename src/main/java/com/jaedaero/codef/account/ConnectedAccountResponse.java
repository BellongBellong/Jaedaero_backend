package com.jaedaero.codef.account;

import com.jaedaero.codef.persistence.StoredConnectedAccount;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "화면에 표시할 수 있도록 민감정보를 제외한 연동 계좌 정보")
public class ConnectedAccountResponse {
    @ApiModelProperty(value = "연동 계좌 ID", example = "10")
    private final long accountId;
    @ApiModelProperty(value = "CODEF 금융기관 코드", example = "0238")
    private final String institutionCode;
    @ApiModelProperty(value = "업무 구분(BK: 은행, ST: 증권)", example = "ST")
    private final String businessType;
    @ApiModelProperty(value = "금융기관 이름", example = "미래에셋증권")
    private final String institutionName;
    @ApiModelProperty(value = "마스킹된 계좌번호", example = "***-***-1234")
    private final String accountMasked;
    @ApiModelProperty(value = "계좌 유형", example = "SECURITIES")
    private final String accountType;
    @ApiModelProperty(value = "계좌 상품명 또는 계좌명", example = "위탁 계좌")
    private final String productName;
    @ApiModelProperty(value = "현재 잔액 또는 평가금액", example = "1500000")
    private final long currentBalance;
    @ApiModelProperty(value = "출금 또는 주문 가능 금액", example = "1200000")
    private final Long availableBalance;
    @ApiModelProperty(value = "만기일(해당하는 상품만 제공)", example = "20270131")
    private final String maturityDate;

    public ConnectedAccountResponse(StoredConnectedAccount account) {
        this.accountId = account.accountId();
        this.institutionCode = account.institutionCode();
        this.businessType = account.businessType();
        this.institutionName = account.institutionName();
        this.accountMasked = account.accountMasked();
        this.accountType = account.accountType();
        this.productName = account.productName();
        this.currentBalance = account.currentBalance();
        this.availableBalance = account.availableBalance();
        this.maturityDate = account.maturityDate();
    }
    public long getAccountId() { return accountId; }
    public String getInstitutionCode() { return institutionCode; }
    public String getBusinessType() { return businessType; }
    public String getInstitutionName() { return institutionName; }
    public String getAccountMasked() { return accountMasked; }
    public String getAccountType() { return accountType; }
    public String getProductName() { return productName; }
    public long getCurrentBalance() { return currentBalance; }
    public Long getAvailableBalance() { return availableBalance; }
    public String getMaturityDate() { return maturityDate; }
}
