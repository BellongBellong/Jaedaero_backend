package com.jaedaero.codef.institution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "프론트엔드 은행 선택 버튼에 사용하는 CODEF 은행 정보")
public class CodefBankInstitutionResponse {

    @ApiModelProperty(value = "CODEF 은행 기관 코드", example = "0004")
    private final String organizationCode;

    @ApiModelProperty(value = "은행 이름", example = "KB국민은행")
    private final String displayName;

    @ApiModelProperty(value = "은행 업무 구분", example = "BK")
    private final String businessType = "BK";

    @ApiModelProperty(value = "개인 고객 구분", example = "P")
    private final String clientType = "P";

    public CodefBankInstitutionResponse(CodefBankInstitution institution) {
        this.organizationCode = institution.getOrganizationCode();
        this.displayName = institution.getDisplayName();
    }

    public String getOrganizationCode() {
        return organizationCode;
    }

    public String getDisplayName() {
        return displayName;
    }

    public String getBusinessType() {
        return businessType;
    }

    public String getClientType() {
        return clientType;
    }
}
