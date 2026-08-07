package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record FredObservationsResponse(
    @JsonProperty("observations") List<FredObservation> observations) {}
