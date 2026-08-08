package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonProperty;

/** KRX 지수 시세정보 원문. */
public record KrxIndexDailyTradingInfo(
    @JsonProperty("BAS_DD") String basDd,
    @JsonProperty("IDX_NM") String idxNm,
    @JsonProperty("CLSPRC_IDX") String clsprcIdx,
    @JsonProperty("CMPPREVDD_IDX") String cmpprevddIdx,
    @JsonProperty("FLUC_RT_IDX") String flucRtIdx) {}
