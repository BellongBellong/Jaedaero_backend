package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FredObservation(
    @JsonProperty("date") String date, @JsonProperty("value") String value) {}
