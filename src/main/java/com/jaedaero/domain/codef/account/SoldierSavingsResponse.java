package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "장병내일준비적금 보유 여부 및 해당 연동 계좌")
public class SoldierSavingsResponse {

  @ApiModelProperty(value = "장병내일준비적금 보유 여부", example = "true")
  private final boolean hasSoldierSavings;

  @ApiModelProperty(value = "장병내일준비적금으로 판별된 계좌 목록")
  private final List<ConnectedAccountResponse> accounts;

  public SoldierSavingsResponse(boolean hasSoldierSavings, List<ConnectedAccountResponse> accounts) {
    this.hasSoldierSavings = hasSoldierSavings;
    this.accounts = List.copyOf(accounts);
  }

  public boolean isHasSoldierSavings() {
    return hasSoldierSavings;
  }

  public List<ConnectedAccountResponse> getAccounts() {
    return accounts;
  }
}
