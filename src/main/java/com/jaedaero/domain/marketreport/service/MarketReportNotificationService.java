package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.service.NotificationCommandService;
import org.springframework.stereotype.Component;

@Component
public class MarketReportNotificationService implements MarketReportPublishedNotifier {
  private final NotificationCommandService notificationCommandService;

  public MarketReportNotificationService(NotificationCommandService notificationCommandService) {
    this.notificationCommandService = notificationCommandService;
  }

  @Override
  public void notifyPublished(DailyMarketReportVo report) {
    notificationCommandService.createCampaign(
        NotificationType.MARKET_REPORT_ARRIVED,
        "오늘의 시장 리포트",
        "오늘의 시장 리포트가 도착했어요",
        "/market-reports/today",
        NotificationCommandService.MARKET_REPORT_TOPIC,
        "market-report:" + report.getReportDate(),
        report.getValidFrom(),
        report.getValidUntil());
  }
}
