package com.jaedaero.domain.marketreport.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.dto.TodayMarketIndicatorsResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportGenerationService;
import com.jaedaero.domain.marketreport.service.MarketReportIndicatorRefreshService;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import java.time.Clock;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;

class MarketReportControllerTest {

  private static final MarketReportService FAKE_SERVICE =
      new MarketReportService() {
        @Override
        public TodayMarketReportResponse getToday() {
          return TodayMarketReportResponse.builder()
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
        }

        @Override
        public TodayMarketIndicatorsResponse getTodayIndicators() {
          return TodayMarketIndicatorsResponse.builder().reportId(1L).indicators(List.of()).build();
        }
      };
  @Test
  void getTodayReturnsCommonReportWithoutUserContext() {
    MarketReportController controller =
        controller(recordingGenerationService(), recordingRefreshService(), "production", "test-token");

    var response = controller.getToday();

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(1L, response.getBody().getReportId());
    assertEquals("오늘의 테스트 시장 리포트", response.getBody().getTitle());
    assertEquals("테스트 요약", response.getBody().getSummary());
  }

  @Test
  void generateNowTriggersServiceWithoutUserContextWhenLocalEnvironment() {
    RecordingGenerationService generationService = recordingGenerationService();
    MarketReportController controller =
        controller(generationService, recordingRefreshService(), "local", "test-token");

    var response = controller.generateNow();

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
        controller(generationService, recordingRefreshService(), "production", "test-token");

    assertThrows(MarketReportException.class, controller::generateNow);
    assertEquals(0, generationService.callCount);
  }

  @Test
  void refreshTodayIndicatorsRequiresMatchingAdminToken() {
    RecordingRefreshService refreshService = recordingRefreshService();
    MarketReportController controller =
        controller(recordingGenerationService(), refreshService, "production", "test-token");

    var response = controller.refreshTodayIndicators("test-token");

    assertEquals(HttpStatus.ACCEPTED, response.getStatusCode());
    assertEquals(1, refreshService.callCount);
    assertThrows(MarketReportException.class, () -> controller.refreshTodayIndicators("wrong-token"));
    assertEquals(1, refreshService.callCount);
  }

  private MarketReportController controller(
      RecordingGenerationService generationService,
      RecordingRefreshService refreshService,
      String environment,
      String token) {
    return new MarketReportController(FAKE_SERVICE, generationService, refreshService, environment, token);
  }

  private RecordingGenerationService recordingGenerationService() {
    return new RecordingGenerationService();
  }

  private RecordingRefreshService recordingRefreshService() {
    return new RecordingRefreshService();
  }

  private static class RecordingGenerationService extends MarketReportGenerationService {
    private int callCount;

    RecordingGenerationService() {
      super(null, null, null, null, null, Clock.systemDefaultZone());
    }

    @Override
    public void generateForTodayForLocalRetry() {
      callCount++;
    }
  }

  private static class RecordingRefreshService extends MarketReportIndicatorRefreshService {
    private int callCount;

    RecordingRefreshService() {
      super(null, null, null, Clock.systemDefaultZone());
    }

    @Override
    public void refreshToday() {
      callCount++;
    }
  }
}
