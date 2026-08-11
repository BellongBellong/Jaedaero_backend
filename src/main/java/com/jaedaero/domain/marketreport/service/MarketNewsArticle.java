package com.jaedaero.domain.marketreport.service;

import java.time.Instant;

public record MarketNewsArticle(
    long id,
    String category,
    Instant publishedAt,
    String headline,
    String summary,
    String source,
    String url) {}
