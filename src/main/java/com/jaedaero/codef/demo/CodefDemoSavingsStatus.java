package com.jaedaero.codef.demo;

import java.util.List;

/** Military savings status inferred from the connected bank account list. */
public class CodefDemoSavingsStatus {

    public enum Result {
        ACTIVE,
        MATURED,
        NOT_FOUND
    }

    private final Result result;
    private final String message;
    private final List<CodefDemoSavingsCard> savingsAccounts;

    public CodefDemoSavingsStatus(Result result, String message, List<CodefDemoSavingsCard> savingsAccounts) {
        this.result = result;
        this.message = message;
        this.savingsAccounts = List.copyOf(savingsAccounts);
    }

    public Result getResult() {
        return result;
    }

    public String getMessage() {
        return message;
    }

    public List<CodefDemoSavingsCard> getSavingsAccounts() {
        return savingsAccounts;
    }

    public boolean isActive() {
        return result == Result.ACTIVE;
    }

    public boolean isMatured() {
        return result == Result.MATURED;
    }

    public boolean isNotFound() {
        return result == Result.NOT_FOUND;
    }
}
