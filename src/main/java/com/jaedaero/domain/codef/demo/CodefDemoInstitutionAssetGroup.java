package com.jaedaero.domain.codef.demo;

import java.util.List;

/** Asset cards grouped by the financial institution that supplied them. */
public class CodefDemoInstitutionAssetGroup {
    private final String institutionName;
    private final List<CodefDemoUnifiedAccountAsset> accounts;

    public CodefDemoInstitutionAssetGroup(String institutionName, List<CodefDemoUnifiedAccountAsset> accounts) {
        this.institutionName = institutionName;
        this.accounts = List.copyOf(accounts);
    }

    public String getInstitutionName() { return institutionName; }
    public List<CodefDemoUnifiedAccountAsset> getAccounts() { return accounts; }
}
