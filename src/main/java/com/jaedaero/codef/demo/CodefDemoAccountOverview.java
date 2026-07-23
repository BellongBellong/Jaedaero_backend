package com.jaedaero.codef.demo;

import java.util.List;

/** Combined account-list result so the JSP demo makes one CODEF account-list request. */
public class CodefDemoAccountOverview {

    private final List<CodefDemoAccountCard> accounts;
    private final CodefDemoSavingsStatus militarySavingsStatus;

    public CodefDemoAccountOverview(
            List<CodefDemoAccountCard> accounts, CodefDemoSavingsStatus militarySavingsStatus) {
        this.accounts = List.copyOf(accounts);
        this.militarySavingsStatus = militarySavingsStatus;
    }

    public List<CodefDemoAccountCard> getAccounts() {
        return accounts;
    }

    public CodefDemoSavingsStatus getMilitarySavingsStatus() {
        return militarySavingsStatus;
    }
}
