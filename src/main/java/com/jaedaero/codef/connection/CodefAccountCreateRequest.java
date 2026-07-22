package com.jaedaero.codef.connection;

/**
 * Input for one CODEF institution registration.
 *
 * <p>Required fields differ by organization and login type. The frontend must use CODEF's
 * organization-specific required parameter guide before submitting this request.
 */
public class CodefAccountCreateRequest {

    private String countryCode = "KR";
    private String businessType = "BK";
    private String clientType = "P";
    private String organization;
    private String loginType;
    private String loginId;
    private String password;
    private String birthday;
    private String keyFile;
    private String derFile;

    public String getCountryCode() {
        return countryCode;
    }

    public void setCountryCode(String countryCode) {
        this.countryCode = countryCode;
    }

    public String getBusinessType() {
        return businessType;
    }

    public void setBusinessType(String businessType) {
        this.businessType = businessType;
    }

    public String getClientType() {
        return clientType;
    }

    public void setClientType(String clientType) {
        this.clientType = clientType;
    }

    public String getOrganization() {
        return organization;
    }

    public void setOrganization(String organization) {
        this.organization = organization;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getLoginId() {
        return loginId;
    }

    public void setLoginId(String loginId) {
        this.loginId = loginId;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getBirthday() {
        return birthday;
    }

    public void setBirthday(String birthday) {
        this.birthday = birthday;
    }

    public String getKeyFile() {
        return keyFile;
    }

    public void setKeyFile(String keyFile) {
        this.keyFile = keyFile;
    }

    public String getDerFile() {
        return derFile;
    }

    public void setDerFile(String derFile) {
        this.derFile = derFile;
    }
}
