package com.jaedaero.domain.marketreport.llm;

import com.jaedaero.domain.marketreport.dto.MarketCondition;

/** LLM이 생성한 오늘의 공통 시장 요약. */
public record MarketReportNarrative(
    MarketCondition marketCondition, String content) {}
