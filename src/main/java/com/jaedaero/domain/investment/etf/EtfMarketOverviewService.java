package com.jaedaero.domain.investment.etf;

import com.jaedaero.domain.investment.exception.KrxApiException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/** 비교 기준일별 일별 응답 하나로 모든 ETF 수익률 요약을 생성합니다. */
@Service
public class EtfMarketOverviewService {

  private static final DateTimeFormatter BASIC_DATE = DateTimeFormatter.BASIC_ISO_DATE;
  private static final int MAX_TRADING_DAY_LOOKUP = 31;

  private final KrxEtfClient krxEtfClient;

  public EtfMarketOverviewService(KrxEtfClient krxEtfClient) {
    this.krxEtfClient = krxEtfClient;
  }

  public EtfMarketOverviewResponse getOverview(LocalDate requestedAsOfDate) {
    List<EtfDailyTradingInfo> asOfItems = findLatestDailyAtOrBefore(requestedAsOfDate);
    if (asOfItems.isEmpty()) {
      throw new KrxApiException("기준일 ETF 데이터를 찾을 수 없습니다.", 404);
    }
    LocalDate asOfDate = LocalDate.parse(asOfItems.get(0).basDd(), BASIC_DATE);
    Map<String, EtfDailyTradingInfo> sixMonths =
        byCode(findEarliestDailyAtOrAfter(asOfDate.minusMonths(6)));
    Map<String, EtfDailyTradingInfo> yearToDate =
        byCode(findEarliestDailyAtOrAfter(asOfDate.withDayOfYear(1)));
    Map<String, EtfDailyTradingInfo> oneYear =
        byCode(findEarliestDailyAtOrAfter(asOfDate.minusYears(1)));
    Map<String, EtfDailyTradingInfo> twoYears =
        byCode(findEarliestDailyAtOrAfter(asOfDate.minusYears(2)));

    List<EtfMarketOverviewItem> items =
        asOfItems.stream()
            .map(
                item ->
                    new EtfMarketOverviewItem(
                        item,
                        List.of(
                            calculate(
                                "SIX_MONTHS",
                                asOfDate.minusMonths(6),
                                item,
                                sixMonths.get(item.isuCd())),
                            calculate(
                                "YEAR_TO_DATE",
                                asOfDate.withDayOfYear(1),
                                item,
                                yearToDate.get(item.isuCd())),
                            calculate(
                                "ONE_YEAR",
                                asOfDate.minusYears(1),
                                item,
                                oneYear.get(item.isuCd())),
                            calculate(
                                "TWO_YEARS",
                                asOfDate.minusYears(2),
                                item,
                                twoYears.get(item.isuCd())))))
            .toList();
    return new EtfMarketOverviewResponse(format(requestedAsOfDate), format(asOfDate), items);
  }

  private EtfReturnPeriod calculate(
      String period,
      LocalDate requestedBaseDate,
      EtfDailyTradingInfo current,
      EtfDailyTradingInfo base) {
    if (base == null) {
      return new EtfReturnPeriod(period, format(requestedBaseDate), null, null, null, false);
    }
    try {
      BigDecimal currentPrice = price(current.tddClsprc());
      BigDecimal basePrice = price(base.tddClsprc());
      BigDecimal returnRate =
          currentPrice
              .subtract(basePrice)
              .multiply(BigDecimal.valueOf(100))
              .divide(basePrice, 2, RoundingMode.HALF_UP);
      return new EtfReturnPeriod(
          period, format(requestedBaseDate), base.basDd(), base.tddClsprc(), returnRate, true);
    } catch (RuntimeException exception) {
      return new EtfReturnPeriod(period, format(requestedBaseDate), null, null, null, false);
    }
  }

  private List<EtfDailyTradingInfo> findLatestDailyAtOrBefore(LocalDate date) {
    for (int daysBack = 0; daysBack <= MAX_TRADING_DAY_LOOKUP; daysBack++) {
      List<EtfDailyTradingInfo> items = itemsOn(date.minusDays(daysBack));
      if (hasTradablePrice(items)) return items;
    }
    return List.of();
  }

  private List<EtfDailyTradingInfo> findEarliestDailyAtOrAfter(LocalDate date) {
    for (int daysForward = 0; daysForward <= MAX_TRADING_DAY_LOOKUP; daysForward++) {
      List<EtfDailyTradingInfo> items = itemsOn(date.plusDays(daysForward));
      if (hasTradablePrice(items)) return items;
    }
    return List.of();
  }

  private List<EtfDailyTradingInfo> itemsOn(LocalDate date) {
    EtfDailyTradingResponse response = krxEtfClient.getDailyTrading(format(date));
    return response.outBlock1() == null ? List.of() : response.outBlock1();
  }

  private Map<String, EtfDailyTradingInfo> byCode(List<EtfDailyTradingInfo> items) {
    Map<String, EtfDailyTradingInfo> result = new HashMap<>();
    items.forEach(item -> result.put(item.isuCd(), item));
    return result;
  }

  /** KRX는 비거래일에 '-' 값으로 채워진 OutBlock_1 목록을 반환할 수 있습니다. */
  private boolean hasTradablePrice(List<EtfDailyTradingInfo> items) {
    return items.stream().anyMatch(item -> isPrice(item.tddClsprc()));
  }

  private boolean isPrice(String value) {
    return value != null && !value.isBlank() && !"-".equals(value);
  }

  private BigDecimal price(String value) {
    return new BigDecimal(value.replace(",", ""));
  }

  private String format(LocalDate date) {
    return date.format(BASIC_DATE);
  }
}
