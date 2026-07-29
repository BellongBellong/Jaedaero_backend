package com.jaedaero.krx.etf;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "ETF 기준일 대비 기간별 수익률 응답")
public record EtfReturnResponse(
        @ApiModelProperty(value = "ETF 종목코드", example = "069500") String isuCd,
        @ApiModelProperty(value = "ETF 종목명", example = "KODEX 200") String isuNm,
        @ApiModelProperty(value = "요청한 기준일", example = "20260728") String requestedAsOfDate,
        @ApiModelProperty(value = "실제 적용된 최신 거래일", example = "20260728") String asOfDate,
        @ApiModelProperty(value = "기준일 종가", example = "50000") String closePrice,
        @ApiModelProperty(value = "6개월·연초 이후·1년·2년 수익률") List<EtfReturnPeriod> returns) {}
