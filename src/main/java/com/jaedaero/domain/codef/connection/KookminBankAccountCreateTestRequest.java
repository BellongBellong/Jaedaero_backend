package com.jaedaero.domain.codef.connection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;

/**
 * KB국민은행 개인 인터넷뱅킹 계좌를 등록하기 위한 개발 전용 입력값입니다.
 *
 * <p>비밀번호는 CODEF 요청을 보낼 때만 사용하며, 절대 저장하거나 로그에 남겨서는 안 됩니다.
 */
@ApiModel(description = "KB국민은행 개인 인터넷뱅킹 Connected ID 발급 테스트 요청")
public class KookminBankAccountCreateTestRequest {

  @NotBlank
  @ApiModelProperty(value = "KB국민은행 인터넷뱅킹 ID", required = true, example = "kb_user_id")
  private String bankLoginId;

  @NotBlank
  @ApiModelProperty(value = "KB국민은행 인터넷뱅킹 비밀번호", required = true, example = "비밀번호는Swagger에서직접입력")
  private String bankPassword;

  @ApiModelProperty(value = "생년월일(기관에서 요구하는 경우 YYMMDD)", example = "990101")
  private String birthDate;

  public String getBankLoginId() {
    return bankLoginId;
  }

  public void setBankLoginId(String bankLoginId) {
    this.bankLoginId = bankLoginId;
  }

  public String getBankPassword() {
    return bankPassword;
  }

  public void setBankPassword(String bankPassword) {
    this.bankPassword = bankPassword;
  }

  public String getBirthDate() {
    return birthDate;
  }

  public void setBirthDate(String birthDate) {
    this.birthDate = birthDate;
  }

  public CodefAccountCreateRequest toCodefRequest() {
    CodefAccountCreateRequest request = new CodefAccountCreateRequest();
    request.setOrganization("0004"); // CODEF KB국민은행 기관코드
    request.setLoginId(bankLoginId);
    request.setPassword(bankPassword);
    request.setBirthDate(birthDate);
    return request;
  }
}
