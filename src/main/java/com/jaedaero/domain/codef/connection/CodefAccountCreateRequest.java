package com.jaedaero.domain.codef.connection;

/**
 * CODEF 기관 하나를 등록하기 위한 입력값입니다.
 *
 * <p>ID/비밀번호 로그인만 지원합니다. 기관별 필수 입력 항목은 CODEF 상품 가이드를 따라야 합니다.
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
