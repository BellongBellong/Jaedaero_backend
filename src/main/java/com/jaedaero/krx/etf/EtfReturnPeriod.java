package com.jaedaero.krx.etf;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.math.BigDecimal;

@ApiModel(description = "ETF 기간별 수익률 정보")
public record EtfReturnPeriod(
        @ApiModelProperty(value = "기간 구분", example = "SIX_MONTHS") String period,
        @ApiModelProperty(value = "계산 기준 예정일", example = "20260128") String requestedBaseDate,
        @ApiModelProperty(value = "실제 적용된 거래일. 해당 기간에 상장 전이면 null", example = "20260128") String baseDate,
        @ApiModelProperty(value = "기준일 종가. 해당 기간에 상장 전이면 null", example = "50000") String baseClosePrice,
        @ApiModelProperty(value = "수익률(%). 해당 기간에 상장 전이면 null", example = "12.34") BigDecimal returnRate,
        @ApiModelProperty(value = "계산 가능 여부", example = "true") boolean available) {}
