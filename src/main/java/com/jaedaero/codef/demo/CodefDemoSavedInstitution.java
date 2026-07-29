package com.jaedaero.codef.demo;

/** A previously connected institution that can be refreshed without entering a password again. */
public class CodefDemoSavedInstitution {
    private final String organizationCode;
    private final String businessType;
    private final String institutionName;
    private final String loginIdDisplay;

    public CodefDemoSavedInstitution(
            String organizationCode, String businessType, String institutionName, String loginIdDisplay) {
        this.organizationCode = organizationCode;
        this.businessType = businessType;
        this.institutionName = institutionName;
        this.loginIdDisplay = loginIdDisplay;
    }

    public String getOrganizationCode() { return organizationCode; }
    public String getBusinessType() { return businessType; }
    public String getInstitutionName() { return institutionName; }
    public String getLoginIdDisplay() { return loginIdDisplay; }
}
