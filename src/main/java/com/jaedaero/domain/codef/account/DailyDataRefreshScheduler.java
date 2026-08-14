package com.jaedaero.domain.codef.account;

import com.jaedaero.domain.auth.service.MilitaryRankRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** 매일 16:30 KST에 계급·CODEF 계좌·거래내역을 미리 동기화합니다. */
@Slf4j
@Component
public class DailyDataRefreshScheduler {

  private final MilitaryRankRefreshService militaryRankRefreshService;
  private final CodefDailySyncService codefDailySyncService;

  public DailyDataRefreshScheduler(
      MilitaryRankRefreshService militaryRankRefreshService,
      CodefDailySyncService codefDailySyncService) {
    this.militaryRankRefreshService = militaryRankRefreshService;
    this.codefDailySyncService = codefDailySyncService;
  }

  @Scheduled(cron = "0 30 16 * * *", zone = "Asia/Seoul")
  public void refreshDailyData() {
    log.info("일일 계급·CODEF 데이터 동기화를 시작합니다.");
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
  }
}
