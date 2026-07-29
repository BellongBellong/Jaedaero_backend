package com.jaedaero.domain.codef.demo;

/** View model for one transaction card in the local JSP demonstration. */
public class CodefDemoTransaction {

    private final String date;
    private final String time;
    private final String description;
    private final String amount;
    private final String balance;
    private final boolean deposit;

    public CodefDemoTransaction(
            String date, String time, String description, String amount, String balance, boolean deposit) {
        this.date = date;
        this.time = time;
        this.description = description;
        this.amount = amount;
        this.balance = balance;
        this.deposit = deposit;
    }

    public String getDate() {
        return date;
    }

    public String getTime() {
        return time;
    }

    public String getDescription() {
        return description;
    }

    public String getAmount() {
        return amount;
    }

    public String getBalance() {
        return balance;
    }

    public boolean isDeposit() {
        return deposit;
    }
}
