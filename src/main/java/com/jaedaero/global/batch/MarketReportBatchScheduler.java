package com.jaedaero.global.batch;

import com.jaedaero.domain.marketreport.service.MarketReportGenerationService;
import com.jaedaero.domain.marketreport.service.MarketReportIndicatorRefreshService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketReportBatchScheduler {

  private final MarketReportGenerationService generationService;
  private final MarketReportIndicatorRefreshService indicatorRefreshService;

  public MarketReportBatchScheduler(
      MarketReportGenerationService generationService,
      MarketReportIndicatorRefreshService indicatorRefreshService) {
    this.generationService = generationService;
    this.indicatorRefreshService = indicatorRefreshService;
  }

  @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Seoul")
  public void generateDailyMarketReport() {
    log.info("오늘의 AI 시장 리포트 배치를 시작합니다.");
    generationService.generateForToday();
  }

  @Scheduled(cron = "0 10,30 17 * * *", zone = "Asia/Seoul")
  public void refreshPartialMarketReportIndicators() {
    if (indicatorRefreshService.refreshTodayIfPartial()) {
      log.info("PARTIAL 시장 리포트의 지표 재수집을 완료했습니다.");
    }
  }
}
