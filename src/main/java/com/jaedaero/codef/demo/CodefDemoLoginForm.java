package com.jaedaero.codef.demo;

import javax.validation.constraints.NotBlank;

/** ID/PW-only login form used by the local JSP demonstration. */
public class CodefDemoLoginForm {

    @NotBlank
    private String organizationCode;

    @NotBlank
    private String loginId;

    @NotBlank
    private String password;

    private String birthday;

    public String getOrganizationCode() {
        return organizationCode;
    }

    public void setOrganizationCode(String organizationCode) {
        this.organizationCode = organizationCode;
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
}
