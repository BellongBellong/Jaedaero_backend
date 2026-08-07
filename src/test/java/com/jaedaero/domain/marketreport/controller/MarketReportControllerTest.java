package com.jaedaero.domain.marketreport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class MarketReportControllerTest {

  @Test
  void getTodayReturnsOkForAuthenticatedUser() {
    MarketReportController controller =
        new MarketReportController(
            () -> TodayMarketReportResponse.builder().reportId(1L).build());
    var authentication =
        new UsernamePasswordAuthenticationToken(
            "7", "password", Collections.emptyList());

    var response = controller.getToday(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1L, response.getBody().getReportId());
  }

  @Test
  void getTodayRejectsMissingAuthentication() {
    MarketReportService service =
        () -> TodayMarketReportResponse.builder().reportId(1L).build();

    assertThrows(
        MarketReportException.class,
        () -> new MarketReportController(service).getToday(null));
  }
}
