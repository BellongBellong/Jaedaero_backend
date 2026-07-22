package com.jaedaero.codef.connection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** Generic personal-bank connection request selected from the frontend institution catalogue. */
@ApiModel(description = "은행 선택 후 CODEF Connected ID를 생성하거나 기존 Connected ID에 은행을 추가하는 요청")
public class CodefBankConnectionCreateRequest {

    @NotBlank
    @ApiModelProperty(value = "GET /api/codef/institutions/banks 응답의 organizationCode", required = true, example = "0004")
    private String organizationCode;

    @NotBlank
    @Pattern(regexp = "[01]", message = "loginType은 0(공동인증서) 또는 1(ID/PW)이어야 합니다.")
    @ApiModelProperty(value = "0: 공동인증서, 1: 인터넷뱅킹 ID/PW", required = true, example = "1")
    private String loginType;

    @ApiModelProperty(value = "기존 Connected ID. 있으면 해당 Connected ID에 선택 은행을 추가합니다.")
    private String connectedId;

    @ApiModelProperty(value = "ID/PW 방식일 때 은행 인터넷뱅킹 ID")
    private String loginId;

    @NotBlank
    @ApiModelProperty(value = "은행 로그인 또는 공동인증서 비밀번호. 저장·로그 출력하지 않습니다.", required = true)
    private String password;

    @ApiModelProperty(value = "기관이 요구하는 경우의 생년월일(YYMMDD)")
    private String birthday;

    @ApiModelProperty(value = "공동인증서 방식일 때 Base64 key 파일")
    private String keyFile;

    @ApiModelProperty(value = "공동인증서 방식일 때 Base64 der 파일")
    private String derFile;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
    }

    public String getLoginType() {
        return loginType;
    }

    public void setLoginType(String loginType) {
        this.loginType = loginType;
    }

    public String getConnectedId() {
        return connectedId;
    }

    public void setConnectedId(String connectedId) {
        this.connectedId = connectedId;
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

    CodefAccountCreateRequest toCodefRequest() {
        CodefAccountCreateRequest request = new CodefAccountCreateRequest();
        request.setOrganization(organizationCode);
        request.setLoginType(loginType);
        request.setLoginId(loginId);
        request.setPassword(password);
        request.setBirthday(birthday);
        request.setKeyFile(keyFile);
        request.setDerFile(derFile);
        return request;
    }
}
