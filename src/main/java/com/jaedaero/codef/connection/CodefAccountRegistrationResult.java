package com.jaedaero.codef.connection;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "CODEF 금융기관 계정 등록 결과")
public class CodefAccountRegistrationResult {

    @ApiModelProperty(value = "CODEF 금융기관 코드", example = "0238")
    private final String organization;
    @ApiModelProperty(value = "CODEF 처리 결과 코드", example = "CF-00000")
    private final String code;
    @ApiModelProperty(value = "CODEF 처리 결과 메시지", example = "성공")
    private final String message;

    public CodefAccountRegistrationResult(String organization, String code, String message) {
        this.organization = organization;
        this.code = code;
        this.message = message;
    }

    public String getOrganization() {
        return organization;
    }

    public String getCode() {
        return code;
    }

    public String getMessage() {
        return message;
    }
}
