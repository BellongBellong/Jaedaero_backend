package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FredObservationsResponse(
    @JsonProperty("observations") List<FredObservation> observations) {}
