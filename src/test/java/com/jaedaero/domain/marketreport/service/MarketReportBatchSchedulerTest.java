package com.jaedaero.domain.marketreport.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.lang.reflect.Method;
import java.time.Clock;
import org.junit.jupiter.api.Test;
import org.springframework.scheduling.annotation.Scheduled;

class MarketReportBatchSchedulerTest {

  @Test
  void scheduledMethodDelegatesToGenerationService() {
    RecordingGenerationService service = new RecordingGenerationService();

    new MarketReportBatchScheduler(service).generateDailyMarketReport();

    assertEquals(1, service.calls);
  }

  @Test
  void generateDailyMarketReportIsScheduledDailyAt17Kst() throws NoSuchMethodException {
    Method method =
        MarketReportBatchScheduler.class.getMethod("generateDailyMarketReport");
    Scheduled scheduled = method.getAnnotation(Scheduled.class);

    assertNotNull(scheduled);
    assertEquals("0 0 17 * * *", scheduled.cron());
    assertEquals("Asia/Seoul", scheduled.zone());
  }

  private static class RecordingGenerationService
      extends MarketReportGenerationService {
    private int calls;

    RecordingGenerationService() {
      super(null, null, null, null, null, Clock.systemUTC());
    }

    @Override
    public void generateForToday() {
      calls++;
    }
  }
}
