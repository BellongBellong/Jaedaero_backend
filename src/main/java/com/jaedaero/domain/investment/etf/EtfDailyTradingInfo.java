package com.jaedaero.domain.investment.etf;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

/** KRX ETF 일별매매정보의 한 종목 데이터입니다. 숫자 형식은 KRX 원문을 보존합니다. */
@ApiModel(description = "KRX ETF 한 종목의 일별매매정보. 숫자 값은 KRX 원문 문자열입니다.")
public record EtfDailyTradingInfo(
    @ApiModelProperty(value = "기준일자", example = "20260728") @JsonProperty("BAS_DD") String basDd,
    @ApiModelProperty(value = "종목코드", example = "069500") @JsonProperty("ISU_CD") String isuCd,
    @ApiModelProperty(value = "종목명", example = "KODEX 200") @JsonProperty("ISU_NM") String isuNm,
    @ApiModelProperty(value = "종가", example = "50000") @JsonProperty("TDD_CLSPRC") String tddClsprc,
    @ApiModelProperty(value = "전일 대비", example = "250") @JsonProperty("CMPPREVDD_PRC")
        String cmpprevddPrc,
    @ApiModelProperty(value = "등락률", example = "0.50") @JsonProperty("FLUC_RT") String flucRt,
    @ApiModelProperty(value = "순자산가치(NAV)", example = "50123.45") @JsonProperty("NAV") String nav,
    @ApiModelProperty(value = "시가", example = "49800") @JsonProperty("TDD_OPNPRC") String tddOpnprc,
    @ApiModelProperty(value = "고가", example = "50300") @JsonProperty("TDD_HGPRC") String tddHgprc,
    @ApiModelProperty(value = "저가", example = "49700") @JsonProperty("TDD_LWPRC") String tddLwprc,
    @ApiModelProperty(value = "거래량", example = "1234567") @JsonProperty("ACC_TRDVOL")
        String accTrdvol,
    @ApiModelProperty(value = "거래대금", example = "61728350000") @JsonProperty("ACC_TRDVAL")
        String accTrdval,
    @ApiModelProperty(value = "시가총액", example = "1000000000000") @JsonProperty("MKTCAP")
        String mktcap,
    @ApiModelProperty(value = "순자산총액", example = "1005000000000")
        @JsonProperty("INVSTASST_NETASST_TOTAMT")
        String invstasstNetasstTotamt,
    @ApiModelProperty(value = "상장좌수", example = "20000000") @JsonProperty("LIST_SHRS")
        String listShrs,
    @ApiModelProperty(value = "기초지수명", example = "코스피 200") @JsonProperty("IDX_IND_NM")
        String idxIndNm,
    @ApiModelProperty(value = "기초지수 종가", example = "350.12") @JsonProperty("OBJ_STKPRC_IDX")
        String objStkprcIdx,
    @ApiModelProperty(value = "기초지수 전일 대비", example = "1.75") @JsonProperty("CMPPREVDD_IDX")
        String cmpprevddIdx,
    @ApiModelProperty(value = "기초지수 등락률", example = "0.50") @JsonProperty("FLUC_RT_IDX")
        String flucRtIdx) {}
