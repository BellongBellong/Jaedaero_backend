package com.jaedaero.codef.demo;

/** View model for one demand-deposit transaction row. */
public class CodefDemoTransaction {

    private final String dateTime;
    private final String description;
    private final String withdrawal;
    private final String deposit;
    private final String balance;

    public CodefDemoTransaction(
            String dateTime, String description, String withdrawal, String deposit, String balance) {
        this.dateTime = dateTime;
        this.description = description;
        this.withdrawal = withdrawal;
        this.deposit = deposit;
        this.balance = balance;
    }

    public String getDateTime() {
        return dateTime;
    }

    public String getDescription() {
        return description;
    }

    public String getWithdrawal() {
        return withdrawal;
    }

    public String getDeposit() {
        return deposit;
    }

    public String getBalance() {
        return balance;
    }
}
