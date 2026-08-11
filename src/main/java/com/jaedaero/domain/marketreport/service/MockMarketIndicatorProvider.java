package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketIndicatorStatus;
import com.jaedaero.domain.marketreport.dto.MarketIndicatorType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * KRX/한국수출입은행/FRED 호출 없이 고정된 목데이터로 지표 4종을 채운다. 로컬에서 gpt-5-nano 요약을
 * 외부 시장 데이터 API 키 없이도 즉시 테스트할 수 있게 한다({@link EnvironmentAwareMarketIndicatorProvider}
 * 가 app.environment=local일 때만 이 구현체를 선택한다).
 */
@Component("mockMarketIndicatorProvider")
public class MockMarketIndicatorProvider implements MarketIndicatorProvider {

  @Override
  public List<MarketIndicatorResult> collect(LocalDate businessDate) {
    return List.of(
        normal(
            MarketIndicatorType.KOSPI,
            businessDate,
            "KRX Open API (로컬 목데이터)",
            "2650.12",
            "12.30",
            "0.47"),
        normal(
            MarketIndicatorType.KOSDAQ,
            businessDate,
            "KRX Open API (로컬 목데이터)",
            "845.30",
            "-3.10",
            "-0.37"),
        normal(
            MarketIndicatorType.USD_KRW,
            businessDate,
            "한국수출입은행 Open API (로컬 목데이터)",
            "1320.50",
            null,
            null),
        normal(
            MarketIndicatorType.US_TREASURY_10Y,
            businessDate,
            "FRED DGS10 (로컬 목데이터)",
            "4.25",
            "0.03",
            "0.71"));
  }

  private MarketIndicatorResult normal(
      MarketIndicatorType type,
      LocalDate dataAsOf,
      String source,
      String observedValue,
      String change,
      String changeRate) {
    MarketIndicatorObservation observation =
        new MarketIndicatorObservation(
            type,
            dataAsOf,
            source,
            new BigDecimal(observedValue),
            change == null ? null : new BigDecimal(change),
            changeRate == null ? null : new BigDecimal(changeRate));
    return MarketIndicatorResult.of(observation, MarketIndicatorStatus.NORMAL);
  }
}
