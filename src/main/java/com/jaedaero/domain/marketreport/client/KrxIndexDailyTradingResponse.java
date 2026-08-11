package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record KrxIndexDailyTradingResponse(
    @JsonProperty("OutBlock_1") List<KrxIndexDailyTradingInfo> outBlock1) {}
