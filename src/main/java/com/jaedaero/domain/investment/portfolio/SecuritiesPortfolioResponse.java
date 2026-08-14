package com.jaedaero.domain.investment.portfolio;

import com.jaedaero.domain.codef.account.SecuritiesHoldingResponse;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "증권 계좌 한 개의 계좌정보·예수금·보유상품·주식보유 현황")
public record SecuritiesPortfolioResponse(
    @ApiModelProperty(value = "연동 계좌 ID", example = "10") long accountId,
    @ApiModelProperty(value = "증권사 이름", example = "미래에셋증권") String institutionName,
    @ApiModelProperty(value = "마스킹된 계좌번호", example = "639602-**-****75") String accountMasked,
    @ApiModelProperty(value = "계좌 상품명", example = "위탁 계좌") String productName,
    @ApiModelProperty(value = "저장된 계좌 평가금액", example = "1500000") long currentBalance,
    @ApiModelProperty(value = "출금 또는 주문 가능 금액", example = "1200000") Long availableBalance,
    @ApiModelProperty(value = "CODEF 실시간 예수금", example = "120000") long depositAmount,
    @ApiModelProperty(value = "주식·펀드 등 전체 보유상품") List<SecuritiesHoldingResponse> holdings,
    @ApiModelProperty(value = "주식 종목 보유정보") List<SecuritiesHoldingResponse> stockHoldings) {
  public SecuritiesPortfolioResponse {
    holdings = List.copyOf(holdings);
    stockHoldings = List.copyOf(stockHoldings);
  }
}
