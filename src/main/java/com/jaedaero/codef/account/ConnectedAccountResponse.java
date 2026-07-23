package com.jaedaero.codef.account;

import com.jaedaero.codef.persistence.StoredConnectedAccount;

/** Account information safe for frontend display. */
public class ConnectedAccountResponse {
    private final long accountId;
    private final String institutionCode;
    private final String institutionName;
    private final String accountMasked;
    private final String accountType;
    private final String productName;
    private final long currentBalance;
    private final Long availableBalance;
    private final String maturityDate;

    public ConnectedAccountResponse(StoredConnectedAccount account) {
        this.accountId = account.accountId();
        this.institutionCode = account.institutionCode();
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
    public String getInstitutionName() { return institutionName; }
    public String getAccountMasked() { return accountMasked; }
    public String getAccountType() { return accountType; }
    public String getProductName() { return productName; }
    public long getCurrentBalance() { return currentBalance; }
    public Long getAvailableBalance() { return availableBalance; }
    public String getMaturityDate() { return maturityDate; }
}
