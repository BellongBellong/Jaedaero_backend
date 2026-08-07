package com.jaedaero.domain.marketreport.dto;

/** 리포트 전체 상태. STALE은 조회 시점에만 부여한다. */
public enum MarketReportStatus {
  NORMAL,
  PARTIAL,
  STALE
}
