package com.jaedaero.domain.codef.connection;

/**
 * Input for one CODEF institution registration.
 *
 * <p>Only ID/password login is supported. Institution-specific required fields must follow the
 * CODEF product guide.
 */
public class CodefAccountCreateRequest {

  public static final String ID_PASSWORD_LOGIN_TYPE = "1";

  private String countryCode = "KR";
  private String businessType = "BK";
  private String clientType = "P";
  private String organization;
  private String loginType = ID_PASSWORD_LOGIN_TYPE;
  private String loginId;
  private String password;
  private String birthDate;

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

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }
}
