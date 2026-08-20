package com.jaedaero.global.batch;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.time.LocalTime;
import org.junit.jupiter.api.Test;

class DailyDataRefreshSchedulerTest {

  @Test
  void startsCatchUpAtDailyRefreshTimeOrLater() {
    assertTrue(DailyDataRefreshScheduler.shouldCatchUpAtStartup(LocalTime.of(16, 30)));
    assertTrue(DailyDataRefreshScheduler.shouldCatchUpAtStartup(LocalTime.of(16, 53, 10)));
  }

  @Test
  void doesNotCatchUpBeforeDailyRefreshTime() {
    assertFalse(DailyDataRefreshScheduler.shouldCatchUpAtStartup(LocalTime.of(16, 29, 59)));
  }
}
