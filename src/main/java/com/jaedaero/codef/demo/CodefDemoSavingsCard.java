package com.jaedaero.codef.demo;

/** View model for a savings account shown in the local CODEF demonstration. */
public class CodefDemoSavingsCard {

    private final String accountName;
    private final String accountDisplay;
    private final String balance;
    private final String maturityDate;
    private final boolean matured;

    public CodefDemoSavingsCard(
            String accountName, String accountDisplay, String balance, String maturityDate, boolean matured) {
        this.accountName = accountName;
        this.accountDisplay = accountDisplay;
        this.balance = balance;
        this.maturityDate = maturityDate;
        this.matured = matured;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getAccountDisplay() {
        return accountDisplay;
    }

    public String getBalance() {
        return balance;
    }

    public String getMaturityDate() {
        return maturityDate;
    }

    public boolean isMatured() {
        return matured;
    }
}
