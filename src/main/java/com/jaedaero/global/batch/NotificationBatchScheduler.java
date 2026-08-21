package com.jaedaero.global.batch;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceService;
import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.mapper.NotificationMapper;
import com.jaedaero.domain.notification.service.NotificationCommandService;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationBatchScheduler {
  private final NotificationMapper notificationMapper;
  private final NotificationCommandService notificationCommandService;
  private final InvestmentGuidanceService investmentGuidanceService;
  private final Clock clock;
  private final int batchSize;

  public NotificationBatchScheduler(
      NotificationMapper notificationMapper,
      NotificationCommandService notificationCommandService,
      InvestmentGuidanceService investmentGuidanceService,
      Clock clock,
      @Value("${NOTIFICATION_BATCH_SIZE:500}") int batchSize) {
    this.notificationMapper = notificationMapper;
    this.notificationCommandService = notificationCommandService;
    this.investmentGuidanceService = investmentGuidanceService;
    this.clock = clock;
    this.batchSize = Math.max(1, batchSize);
  }

  @Scheduled(cron = "0 30 17 * * *", zone = "Asia/Seoul")
  public void notifyDailyMissionAvailable() {
    LocalDate today = LocalDate.now(clock);
    long createdCount = processUsers(
        notificationMapper::findDailyMissionTargetUserIds,
        userId ->
            notificationCommandService.createUserNotification(
                userId,
                NotificationType.DAILY_MISSION_AVAILABLE,
                "오늘의 미션이 도착했어요",
                "오늘 완료할 수 있는 투자 미션을 확인해 보세요.",
                "/missions/today",
                "daily-mission:" + userId + ":" + today));
    log.info("오늘의 미션 알림 생성이 완료되었습니다. date={}, count={}", today, createdCount);
  }

  @Scheduled(cron = "0 0 18 28 * *", zone = "Asia/Seoul")
  public void generateMonthlyInvestmentReports() {
    YearMonth month = YearMonth.now(clock);
    BatchResult result = processInvestmentGuidanceUsers(month);
    log.info(
        "월간 적립식 투자 리포트 배치가 완료되었습니다. month={}, success={}, failure={}",
        month,
        result.successCount(),
        result.failureCount());
  }

  private BatchResult processInvestmentGuidanceUsers(YearMonth month) {
    long lastUserId = 0L;
    long successCount = 0L;
    long failureCount = 0L;
    while (true) {
      List<Long> userIds =
          notificationMapper.findActiveInvestmentPlanUserIds(lastUserId, batchSize);
      if (userIds.isEmpty()) {
        return new BatchResult(successCount, failureCount);
      }
      for (Long userId : userIds) {
        try {
          InvestmentGuidanceResponse guidance = investmentGuidanceService.create(userId);
          notificationCommandService.createUserNotification(
              userId,
              NotificationType.MONTHLY_INVESTMENT_REPORT_ARRIVED,
              "적립식 투자 리포트가 도착했어요",
              "이번 달 적립식 투자 가이드와 추천 금액을 확인해 보세요.",
              "/investment-guidance/" + guidance.getGuidanceId(),
              "monthly-investment-report:" + userId + ":" + month);
          successCount++;
        } catch (RuntimeException exception) {
          failureCount++;
          log.debug("월간 적립식 투자 리포트 생성에 실패했습니다. userId={}", userId, exception);
        }
      }
      lastUserId = userIds.get(userIds.size() - 1);
      if (userIds.size() < batchSize) {
        return new BatchResult(successCount, failureCount);
      }
    }
  }

  private long processUsers(UserPageFinder finder, UserProcessor processor) {
    long lastUserId = 0L;
    long createdCount = 0L;
    while (true) {
      List<Long> userIds = finder.find(lastUserId, batchSize);
      if (userIds.isEmpty()) {
        return createdCount;
      }
      for (Long userId : userIds) {
        try {
          processor.process(userId);
          createdCount++;
        } catch (RuntimeException exception) {
          log.debug("사용자 알림 생성에 실패했습니다. userId={}", userId, exception);
        }
      }
      lastUserId = userIds.get(userIds.size() - 1);
      if (userIds.size() < batchSize) {
        return createdCount;
      }
    }
  }

  @FunctionalInterface
  private interface UserPageFinder {
    List<Long> find(long lastUserId, int limit);
  }

  @FunctionalInterface
  private interface UserProcessor {
    void process(long userId);
  }

  private record BatchResult(long successCount, long failureCount) {}
}
