package com.jaedaero.domain.leavebenefit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "휴가 혜택 카테고리")
public enum LeaveBenefitCategory {
  @ApiModelProperty(value = "교통")
  TRANSPORT,
  @ApiModelProperty(value = "여가")
  LEISURE,
  @ApiModelProperty(value = "자기계발")
  SELF_DEVELOPMENT,
  @ApiModelProperty(value = "숙박")
  LODGING,
  @ApiModelProperty(value = "기타")
  ETC
}
