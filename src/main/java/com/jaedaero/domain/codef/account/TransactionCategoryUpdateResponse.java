package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "수정된 거래내역 카테고리")
public class TransactionCategoryUpdateResponse {
  @ApiModelProperty(value = "거래내역 ID", example = "100")
  private final long transactionId;

  @ApiModelProperty(value = "저장된 카테고리 코드", example = "FOOD")
  private final TransactionCategory category;

  @ApiModelProperty(value = "화면 표시용 카테고리명", example = "식비")
  private final String categoryName;

  public TransactionCategoryUpdateResponse(long transactionId, TransactionCategory category) {
    this.transactionId = transactionId;
    this.category = category;
    this.categoryName = category.getDisplayName();
  }

  public long getTransactionId() {
    return transactionId;
  }

  public TransactionCategory getCategory() {
    return category;
  }

  public String getCategoryName() {
    return categoryName;
  }
}
