package com.jaedaero.domain.marketreport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportGenerationService;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import java.time.Clock;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class MarketReportControllerTest {

  private static final MarketReportService FAKE_SERVICE =
      () ->
          TodayMarketReportResponse.builder()
              .reportId(1L)
              .title("오늘의 테스트 시장 리포트")
              .summary("테스트 요약")
              .generationSource(MarketReportGenerationSource.GEMINI)
              .sources(
                  List.of(
                      MarketReportSourceItem.builder()
                          .title("테스트 출처")
                          .url("https://example.com/test")
                          .build()))
              .build();
  private static final UsernamePasswordAuthenticationToken AUTHENTICATED =
      new UsernamePasswordAuthenticationToken("7", "password", Collections.emptyList());

  @Test
  void getTodayReturnsOkForAuthenticatedUser() {
    MarketReportController controller =
        new MarketReportController(FAKE_SERVICE, recordingGenerationService(), "production");

    var response = controller.getToday(AUTHENTICATED);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1L, response.getBody().getReportId());
    assertEquals("오늘의 테스트 시장 리포트", response.getBody().getTitle());
    assertEquals("테스트 요약", response.getBody().getSummary());
  }

  @Test
  void getTodayRejectsMissingAuthentication() {
    MarketReportController controller =
        new MarketReportController(FAKE_SERVICE, recordingGenerationService(), "production");

    assertThrows(MarketReportException.class, () -> controller.getToday(null));
  }

  @Test
  void getTodayRejectsAnonymousAuthentication() {
    MarketReportController controller =
        new MarketReportController(FAKE_SERVICE, recordingGenerationService(), "production");
    var anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", java.util.List.of(new org.springframework.security.core.authority.SimpleGrantedAuthority("ROLE_ANONYMOUS")));

    assertThrows(MarketReportException.class, () -> controller.getToday(anonymous));
  }

  @Test
  void generateNowTriggersServiceWhenLocalEnvironment() {
    RecordingGenerationService generationService = recordingGenerationService();
    MarketReportController controller =
        new MarketReportController(FAKE_SERVICE, generationService, "local");

    var response = controller.generateNow(AUTHENTICATED);

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertEquals(1L, response.getBody().getReportId());
    assertEquals(MarketReportGenerationSource.GEMINI, response.getBody().getGenerationSource());
    assertEquals(1, response.getBody().getSources().size());
    assertEquals(1, generationService.callCount);
  }

  @Test
  void generateNowRejectsNonLocalEnvironment() {
    RecordingGenerationService generationService = recordingGenerationService();
    MarketReportController controller =
        new MarketReportController(FAKE_SERVICE, generationService, "production");

    assertThrows(MarketReportException.class, () -> controller.generateNow(AUTHENTICATED));
    assertEquals(0, generationService.callCount);
  }

  private RecordingGenerationService recordingGenerationService() {
    return new RecordingGenerationService();
  }

  private static class RecordingGenerationService extends MarketReportGenerationService {
    private int callCount;

    RecordingGenerationService() {
      super(null, null, null, null, null, null, Clock.systemDefaultZone());
    }

    @Override
    public void generateForTodayForLocalRetry() {
      callCount++;
    }
  }
}
