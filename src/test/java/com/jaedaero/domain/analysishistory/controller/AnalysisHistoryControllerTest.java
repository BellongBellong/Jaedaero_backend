package com.jaedaero.domain.analysishistory.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryFilter;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryPageResponse;
import com.jaedaero.domain.analysishistory.exception.AnalysisHistoryException;
import com.jaedaero.domain.analysishistory.service.AnalysisHistoryService;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class AnalysisHistoryControllerTest {

  @Test
  void authenticatedUserAndQueryArePassedToService() {
    RecordingService service = new RecordingService();
    AnalysisHistoryController controller = new AnalysisHistoryController(service);
    var authentication =
        new UsernamePasswordAuthenticationToken("7", "password", Collections.emptyList());

    var response = controller.getHistories(authentication, AnalysisHistoryFilter.WHAT_IF, 2, 10);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(7L, service.userId);
    assertEquals(AnalysisHistoryFilter.WHAT_IF, service.type);
    assertEquals(2, service.page);
    assertEquals(10, service.size);
  }

  @Test
  void missingAuthenticationIsRejected() {
    AnalysisHistoryController controller = new AnalysisHistoryController(new RecordingService());

    assertThrows(
        AnalysisHistoryException.class,
        () -> controller.getHistories(null, AnalysisHistoryFilter.ALL, 0, 20));
  }

  private static class RecordingService implements AnalysisHistoryService {

    private long userId;
    private AnalysisHistoryFilter type;
    private int page;
    private int size;

    @Override
    public AnalysisHistoryPageResponse getHistories(
        long userId, AnalysisHistoryFilter type, int page, int size) {
      this.userId = userId;
      this.type = type;
      this.page = page;
      this.size = size;
      return AnalysisHistoryPageResponse.builder()
          .histories(List.of())
          .page(page)
          .size(size)
          .totalCount(0L)
          .hasNext(false)
          .build();
    }
  }
}
