package com.jaedaero.domain.marketreport.client;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record FinnhubNewsItem(
    String category,
    long datetime,
    String headline,
    long id,
    String image,
    String related,
    String source,
    String summary,
    String url) {}
