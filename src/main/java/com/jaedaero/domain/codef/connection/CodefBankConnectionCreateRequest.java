package com.jaedaero.domain.codef.connection;

import com.jaedaero.domain.codef.institution.CodefBusinessType;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

/** ID/password-only request for one bank or securities institution connection. */
@ApiModel(description = "온라인 거래 ID와 비밀번호로 금융기관을 CODEF에 연결하는 요청")
public class CodefBankConnectionCreateRequest {

  @NotNull
  @ApiModelProperty(
      value = "임시 사용자 식별자. JWT 도입 후 인증 사용자 ID로 대체합니다.",
      required = true,
      example = "1")
  private Long userId;

  @NotBlank
  @ApiModelProperty(value = "금융기관 목록 조회 응답에서 받은 기관 코드", required = true, example = "0238")
  private String organizationCode;

  @NotBlank
  @Pattern(regexp = "BK|ST", message = "businessType은 BK(은행) 또는 ST(증권)이어야 합니다.")
  @ApiModelProperty(value = "CODEF 업무 구분. BK: 은행, ST: 증권", example = "BK")
  private String businessType = CodefBusinessType.BANK.getCode();

  @NotBlank
  @ApiModelProperty(value = "은행 또는 증권사 온라인 거래 ID", required = true)
  private String loginId;

  @NotBlank
  @ApiModelProperty(value = "온라인 거래 비밀번호. 저장·로그 출력하지 않습니다.", required = true)
  private String password;

  @ApiModelProperty(value = "기관이 요구하는 경우의 생년월일(YYMMDD). CODEF 필드명: birthDate")
  private String birthDate;

  public Long getUserId() {
    return userId;
  }

  public void setUserId(Long userId) {
    this.userId = userId;
  }

  public String getOrganizationCode() {
    return organizationCode;
  }

  public void setOrganizationCode(String organizationCode) {
    this.organizationCode = organizationCode;
  }

  public String getBusinessType() {
    return businessType;
  }

  public void setBusinessType(String businessType) {
    this.businessType = businessType;
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

  CodefAccountCreateRequest toCodefRequest() {
    CodefAccountCreateRequest request = new CodefAccountCreateRequest();
    request.setBusinessType(businessType);
    // CODEF Connected ID 명세: 증권(ST)은 개인(P)이 아닌 통합 고객구분(A)을 사용한다.
    request.setClientType(CodefBusinessType.SECURITIES.getCode().equals(businessType) ? "A" : "P");
    request.setOrganization(organizationCode);
    request.setLoginId(loginId);
    request.setPassword(password);
    request.setBirthDate(birthDate);
    return request;
  }
}
