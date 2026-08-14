package com.jaedaero.domain.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.dashboard.mapper.AssetSnapshotMapper;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class AssetSnapshotServiceTest {

  @Test
  void savesSnapshotForTodayInKoreanTime() {
    RecordingDashboardMapper mapper = new RecordingDashboardMapper();
    AssetSnapshotService service =
        new AssetSnapshotService(
            mapper,
            Clock.fixed(Instant.parse("2026-08-14T15:40:00Z"), ZoneId.of("Asia/Seoul")));

    assertEquals(2, service.snapshotToday());
    assertEquals(LocalDate.of(2026, 8, 15), mapper.snapshotDate);
  }

  private static class RecordingDashboardMapper implements AssetSnapshotMapper {
    private LocalDate snapshotDate;

    @Override
    public int upsertDailyAssetSnapshots(LocalDate date) {
      snapshotDate = date;
      return 2;
    }
  }
}
