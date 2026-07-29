package com.jaedaero.domain.codef.account;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "증권 계좌의 상품 또는 종목별 보유 정보")
public record SecuritiesHoldingResponse(
    @ApiModelProperty(value = "상품 유형", example = "주식") String productType,
    @ApiModelProperty(value = "상품 유형 코드", example = "01") String productTypeCode,
    @ApiModelProperty(value = "상품 또는 종목명", example = "삼성전자") String itemName,
    @ApiModelProperty(value = "상품 또는 종목 코드", example = "005930") String itemCode,
    @ApiModelProperty(value = "보유 수량", example = "10") String quantity,
    @ApiModelProperty(value = "매입 금액", example = "700000") long purchaseAmount,
    @ApiModelProperty(value = "평가 금액", example = "750000") long valuationAmount,
    @ApiModelProperty(value = "평가 손익", example = "50000") long valuationProfit,
    @ApiModelProperty(value = "수익률", example = "7.14") String earningsRate,
    @ApiModelProperty(value = "현재가", example = "75000") long currentPrice,
    @ApiModelProperty(value = "통화 코드", example = "KRW") String currency) {}
