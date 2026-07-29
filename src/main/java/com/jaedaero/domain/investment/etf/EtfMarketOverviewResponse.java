package com.jaedaero.domain.investment.etf;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "기준일 전체 ETF 시세 및 기간별 수익률 요약 응답")
public record EtfMarketOverviewResponse(
        @ApiModelProperty(value = "요청한 기준일", example = "20260729") String requestedAsOfDate,
        @ApiModelProperty(value = "실제 적용된 거래일", example = "20260728") String asOfDate,
        @ApiModelProperty(value = "전체 ETF 시세 및 수익률") List<EtfMarketOverviewItem> items) {}
