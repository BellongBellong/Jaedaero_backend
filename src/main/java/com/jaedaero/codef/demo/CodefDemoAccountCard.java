package com.jaedaero.codef.demo;

/** View model rendered as one account card in the JSP demonstration. */
public class CodefDemoAccountCard {

    private final String account;
    private final String accountDisplay;
    private final String accountName;
    private final String category;
    private final String balance;
    private final boolean transactionSupported;

    public CodefDemoAccountCard(
            String account,
            String accountDisplay,
            String accountName,
            String category,
            String balance,
            boolean transactionSupported) {
        this.account = account;
        this.accountDisplay = accountDisplay;
        this.accountName = accountName;
        this.category = category;
        this.balance = balance;
        this.transactionSupported = transactionSupported;
    }

    public String getAccount() {
        return account;
    }

    public String getAccountDisplay() {
        return accountDisplay;
    }

    public String getAccountName() {
        return accountName;
    }

    public String getCategory() {
        return category;
    }

    public String getBalance() {
        return balance;
    }

    public boolean isTransactionSupported() {
        return transactionSupported;
    }
}
