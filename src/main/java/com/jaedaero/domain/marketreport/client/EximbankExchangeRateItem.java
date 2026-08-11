package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record EximbankExchangeRateItem(
    @JsonProperty("result") int result,
    @JsonProperty("cur_unit") String curUnit,
    @JsonProperty("cur_nm") String curNm,
    @JsonProperty("deal_bas_r") String dealBasR) {}
