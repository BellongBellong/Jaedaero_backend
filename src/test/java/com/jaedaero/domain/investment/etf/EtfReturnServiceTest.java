package com.jaedaero.domain.investment.etf;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class EtfReturnServiceTest {

  @Test
  void getReturns_usesPreviousTradingDayForAsOfAndNextTradingDayForStartDates() {
    EtfReturnService service =
        new EtfReturnService(
            new StubKrxEtfClient(
                Map.of(
                    "20260724", item("20260724", "120"),
                    "20260126", item("20260126", "100"),
                    "20260101", item("20260101", "-"),
                    "20260102", item("20260102", "96"),
                    "20250724", item("20250724", "80"),
                    "20240724", item("20240724", "60"))));

    EtfReturnResponse response = service.getReturns("069500", LocalDate.of(2026, 7, 26));

    assertEquals("20260724", response.asOfDate());
    assertEquals("20260126", response.returns().get(0).baseDate());
    assertEquals("20260102", response.returns().get(1).baseDate());
    assertEquals(new BigDecimal("20.00"), response.returns().get(0).returnRate());
    assertEquals(new BigDecimal("25.00"), response.returns().get(1).returnRate());
    assertEquals(new BigDecimal("50.00"), response.returns().get(2).returnRate());
    assertEquals(new BigDecimal("100.00"), response.returns().get(3).returnRate());
  }

  private static EtfDailyTradingInfo item(String date, String closePrice) {
    return new EtfDailyTradingInfo(
        date,
        "069500",
        "KODEX 200",
        closePrice,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private static class StubKrxEtfClient extends KrxEtfClient {
    private final Map<String, EtfDailyTradingInfo> items;

    StubKrxEtfClient(Map<String, EtfDailyTradingInfo> items) {
      super(
          URI.create("http://localhost"), "unused", HttpClient.newHttpClient(), new ObjectMapper());
      this.items = items;
    }

    @Override
    public EtfDailyTradingResponse getDailyTrading(String basDd) {
      EtfDailyTradingInfo item = items.get(basDd);
      return new EtfDailyTradingResponse(item == null ? List.of() : List.of(item));
    }
  }
}
