package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import org.junit.jupiter.api.Test;

class MarketReportBatchSchedulerTest {

  @Test
  void scheduledMethodDelegatesToGenerationService() {
    RecordingGenerationService service = new RecordingGenerationService();

    new MarketReportBatchScheduler(service).generateDailyMarketReport();

    assertEquals(1, service.calls);
  }

  private static class RecordingGenerationService
      extends MarketReportGenerationService {
    private int calls;

    RecordingGenerationService() {
      super(null, null, null, null, Clock.systemUTC());
    }

    @Override
    public void generateForToday() {
      calls++;
    }
  }
}
