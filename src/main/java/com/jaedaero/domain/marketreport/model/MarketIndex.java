package com.jaedaero.domain.marketreport.model;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 금융위원회 API의 문자열 응답을 시장 리포트 내부에서 사용하는 타입으로 변환한 지표. */
public record MarketIndex(
    LocalDate baseDate,
    String indexName,
    BigDecimal closingPrice,
    BigDecimal change,
    BigDecimal changeRate) {}
