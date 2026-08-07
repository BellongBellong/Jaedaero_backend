package com.jaedaero.domain.marketreport.service;

import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class MarketReportBatchScheduler {

  private final MarketReportGenerationService generationService;

  public MarketReportBatchScheduler(
      MarketReportGenerationService generationService) {
    this.generationService = generationService;
  }

  @Scheduled(cron = "0 0 17 * * *", zone = "Asia/Seoul")
  public void generateDailyMarketReport() {
    log.info("오늘의 AI투자리포트 배치를 시작합니다.");
    generationService.generateForToday();
  }
}
