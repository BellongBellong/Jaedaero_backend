package com.jaedaero.domain.codef.connection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;

/**
 * Development-only input for registering a personal KB Kookmin Bank internet-banking account.
 *
 * <p>The password is used only to make the CODEF request and must never be persisted or logged.
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
