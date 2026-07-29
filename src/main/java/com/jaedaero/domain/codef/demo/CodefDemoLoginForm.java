package com.jaedaero.domain.codef.demo;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

/** ID/PW-only login form used by the local JSP demonstration. */
public class CodefDemoLoginForm {

    @NotBlank
    private String organizationCode;

    @NotBlank
    @Pattern(regexp = "BK|ST", message = "연결 구분은 은행 또는 증권이어야 합니다.")
    private String businessType = "BK";

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    private String birthDate;

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
}
