package com.jaedaero.codef.demo;

/** One account's contribution to the all-in-one asset lookup. */
public class CodefDemoUnifiedAccountAsset {
    private final long accountId;
    private final String institutionName;
    private final String accountName;
    private final String accountDisplay;
    private final String assetAmount;
    private final String statusMessage;
    private final boolean securities;
    private final boolean bankDetailAvailable;
    private final String transactionKind;

    public CodefDemoUnifiedAccountAsset(
            long accountId, String institutionName, String accountName, String accountDisplay, String assetAmount,
            String statusMessage, boolean securities, boolean bankDetailAvailable, String transactionKind) {
        this.accountId = accountId;
        this.institutionName = institutionName;
        this.accountName = accountName;
        this.accountDisplay = accountDisplay;
        this.assetAmount = assetAmount;
        this.statusMessage = statusMessage;
        this.securities = securities;
        this.bankDetailAvailable = bankDetailAvailable;
        this.transactionKind = transactionKind;
    }

    public long getAccountId() { return accountId; }
    public String getInstitutionName() { return institutionName; }
    public String getAccountName() { return accountName; }
    public String getAccountDisplay() { return accountDisplay; }
    public String getAssetAmount() { return assetAmount; }
    public String getStatusMessage() { return statusMessage; }
    public boolean isSecurities() { return securities; }
    public boolean isBankDetailAvailable() { return bankDetailAvailable; }
    public String getTransactionKind() { return transactionKind; }
}
