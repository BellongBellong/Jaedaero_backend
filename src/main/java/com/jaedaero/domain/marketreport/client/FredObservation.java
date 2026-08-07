package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonProperty;

public record FredObservation(
    @JsonProperty("date") String date, @JsonProperty("value") String value) {}
