package com.jaedaero.global.batch;

import com.jaedaero.domain.auth.service.MilitaryRankRefreshService;
import com.jaedaero.domain.codef.account.CodefDailySyncService;
import com.jaedaero.domain.dashboard.service.AssetSnapshotService;
import java.time.Clock;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicBoolean;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 16:30 KST에 계급·CODEF 데이터·자산 스냅샷을 순서대로 갱신합니다. */
@Slf4j
@Component
public class DailyDataRefreshScheduler {

  private static final ZoneId KST = ZoneId.of("Asia/Seoul");
  private static final LocalTime DAILY_REFRESH_TIME = LocalTime.of(16, 30);

  private final MilitaryRankRefreshService militaryRankRefreshService;
  private final CodefDailySyncService codefDailySyncService;
  private final AssetSnapshotService assetSnapshotService;
  private final Clock clock;
  private final AtomicBoolean startupCatchUpChecked = new AtomicBoolean();

  public DailyDataRefreshScheduler(
      MilitaryRankRefreshService militaryRankRefreshService,
      CodefDailySyncService codefDailySyncService,
      AssetSnapshotService assetSnapshotService,
      Clock clock) {
    this.militaryRankRefreshService = militaryRankRefreshService;
    this.codefDailySyncService = codefDailySyncService;
    this.assetSnapshotService = assetSnapshotService;
    this.clock = clock;
  }

  /**
   * 배포 등으로 서버가 16:30 이후에 기동되면 그날의 일일 동기화를 한 번 보정한다.
   *
   * <p>루트 컨텍스트가 준비된 뒤에만 실행하고, 한 JVM 안에서는 한 번만 검사한다. 일일 자산 스냅샷은
   * upsert 방식이므로 보정 실행으로 같은 날짜의 스냅샷이 중복 생성되지는 않는다.
   */
  @EventListener(ContextRefreshedEvent.class)
  public void catchUpMissedDailyRefreshOnStartup(ContextRefreshedEvent event) {
    if (event.getApplicationContext().getParent() != null
        || !startupCatchUpChecked.compareAndSet(false, true)) {
      return;
    }

    LocalTime now = LocalTime.now(clock.withZone(KST));
    if (!shouldCatchUpAtStartup(now)) {
      return;
    }

    log.warn("서버가 일일 동기화 시각 이후에 기동되어 보정 동기화를 실행합니다. currentTime={}", now);
    refreshDailyData();
  }

  @Scheduled(cron = "0 30 16 * * *", zone = "Asia/Seoul")
  public void refreshDailyData() {
    log.info("일일 계급·CODEF 데이터·자산 스냅샷 동기화를 시작합니다.");
    try {
      log.info("현재 계급 {}건을 갱신했습니다.", militaryRankRefreshService.refreshCurrentRanks());
    } catch (RuntimeException exception) {
      log.error("일일 계급 갱신에 실패했습니다.", exception);
    }
    try {
      codefDailySyncService.syncAllActiveConnections();
    } catch (RuntimeException exception) {
      log.error("일일 CODEF 데이터 동기화에 실패했습니다.", exception);
    }
    try {
      log.info("일일 자산 스냅샷 {}건을 저장했습니다.", assetSnapshotService.snapshotToday());
    } catch (RuntimeException exception) {
      log.error("일일 자산 스냅샷 저장에 실패했습니다.", exception);
    }
  }

  static boolean shouldCatchUpAtStartup(LocalTime currentTime) {
    return !currentTime.isBefore(DAILY_REFRESH_TIME);
  }
}
