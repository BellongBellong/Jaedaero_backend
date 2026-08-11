package com.jaedaero.domain.marketreport.client;

/** 금융위원회 주가지수시세 응답에서 시장 리포트에 사용하는 필드. */
public record FinancialMarketIndexItem(
    String basDt, String idxNm, String clpr, String vs, String fltRt) {}
