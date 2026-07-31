package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import javax.validation.constraints.NotNull;

@ApiModel(description = "거래내역 카테고리 수정 요청")
public class TransactionCategoryUpdateRequest {

  @NotNull(message = "category는 필수입니다.")
  @ApiModelProperty(
      value = "카테고리 코드. SALARY, ASSET, PX, FOOD, SHOPPING, TRANSPORT, LEISURE, MEDICAL, ETC 중 하나",
      required = true,
      example = "FOOD",
      allowableValues = "SALARY,ASSET,PX,FOOD,SHOPPING,TRANSPORT,LEISURE,MEDICAL,ETC")
  private TransactionCategory category;

  public TransactionCategory getCategory() {
    return category;
  }

  public void setCategory(TransactionCategory category) {
    this.category = category;
  }
}
