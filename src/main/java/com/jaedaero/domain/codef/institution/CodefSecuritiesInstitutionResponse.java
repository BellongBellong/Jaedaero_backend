package com.jaedaero.domain.codef.institution;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "프론트엔드 증권사 선택에 사용하는 CODEF 증권사 정보")
public class CodefSecuritiesInstitutionResponse {

  @ApiModelProperty(value = "CODEF 증권사 기관 코드", example = "0238")
  private final String organizationCode;

  @ApiModelProperty(value = "증권사 이름", example = "미래에셋증권")
  private final String displayName;

  @ApiModelProperty(value = "증권 업무 구분", example = "ST")
  private final String businessType = "ST";

  public CodefSecuritiesInstitutionResponse(CodefSecuritiesInstitution institution) {
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
}
