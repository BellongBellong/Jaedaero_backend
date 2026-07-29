package com.jaedaero.domain.investment.etf;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "ETF 현재 시세와 기간별 수익률 요약")
public record EtfMarketOverviewItem(
    @ApiModelProperty(value = "ETF 일별매매정보") EtfDailyTradingInfo etf,
    @ApiModelProperty(value = "6개월·연초 이후·1년·2년 수익률") List<EtfReturnPeriod> returns) {}
