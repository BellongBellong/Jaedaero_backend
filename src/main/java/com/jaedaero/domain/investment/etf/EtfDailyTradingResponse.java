package com.jaedaero.domain.investment.etf;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

/** KRX ETF 일별매매정보 응답입니다. */
@ApiModel(description = "KRX ETF 일별매매정보 응답")
public record EtfDailyTradingResponse(
        @ApiModelProperty(value = "ETF 일별매매정보 목록") @JsonProperty("OutBlock_1") List<EtfDailyTradingInfo> outBlock1) {}
