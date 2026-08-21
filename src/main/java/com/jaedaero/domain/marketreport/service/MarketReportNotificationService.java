package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.service.NotificationCommandService;
import org.springframework.stereotype.Component;

@Component
public class MarketReportNotificationService implements MarketReportPublishedNotifier {
  private static final int MAX_BODY_LENGTH = 500;

  private final NotificationCommandService notificationCommandService;

  public MarketReportNotificationService(NotificationCommandService notificationCommandService) {
    this.notificationCommandService = notificationCommandService;
  }

  @Override
  public void notifyPublished(DailyMarketReportVo report) {
    notificationCommandService.createCampaign(
        NotificationType.MARKET_REPORT_ARRIVED,
        "오늘의 AI 시장 리포트가 도착했어요",
        abbreviate(report.getSummary(), MAX_BODY_LENGTH),
        "/market-reports/today",
        NotificationCommandService.MARKET_REPORT_TOPIC,
        "market-report:" + report.getReportDate(),
        report.getValidFrom(),
        report.getValidUntil());
  }

  private String abbreviate(String value, int maximumLength) {
    if (value == null || value.isBlank()) {
      return "오늘의 주요 시장 뉴스를 확인해 보세요.";
    }
    int length = value.codePointCount(0, value.length());
    if (length <= maximumLength) {
      return value;
    }
    int end = value.offsetByCodePoints(0, maximumLength - 1);
    return value.substring(0, end) + "…";
  }
}
