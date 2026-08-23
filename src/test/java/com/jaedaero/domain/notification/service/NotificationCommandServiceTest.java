package com.jaedaero.domain.notification.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.mapper.NotificationMapper;
import com.jaedaero.domain.notification.mapper.NotificationOutboxMapper;
import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationFeedRow;
import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import com.jaedaero.domain.notification.vo.NotificationVo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class NotificationCommandServiceTest {
  private static final Clock CLOCK =
      Clock.fixed(Instant.parse("2026-08-21T08:30:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void personalNotificationCreatesLinkedOutbox() {
    RecordingNotificationMapper notifications = new RecordingNotificationMapper();
    RecordingOutboxMapper outboxes = new RecordingOutboxMapper();
    NotificationCommandService service = new NotificationCommandService(notifications, outboxes, CLOCK);

    long id =
        service.createUserNotification(
            7L,
            NotificationType.MISSION_COMPLETED,
            "미션 완료",
            "오늘의 미션을 완료했습니다.",
            "/missions/today",
            "mission-completed:7:3:2026-08-21");

    assertEquals(11L, id);
    assertEquals(7L, notifications.notification.getUserId());
    assertEquals(11L, outboxes.outbox.getNotificationId());
    assertEquals(null, outboxes.outbox.getCampaignId());
    assertEquals("outbox:mission-completed:7:3:2026-08-21", outboxes.outbox.getDedupeKey());
    assertNotNull(outboxes.outbox.getEventId());
    assertEquals(LocalDateTime.of(2026, 8, 21, 17, 30), outboxes.outbox.getAvailableAt());
  }

  @Test
  void campaignCreatesTopicLinkedOutbox() {
    RecordingNotificationMapper notifications = new RecordingNotificationMapper();
    RecordingOutboxMapper outboxes = new RecordingOutboxMapper();
    NotificationCommandService service = new NotificationCommandService(notifications, outboxes, CLOCK);

    long id =
        service.createCampaign(
            NotificationType.MARKET_REPORT_ARRIVED,
            "시장 리포트 도착",
            "오늘의 시장 뉴스를 확인하세요.",
            "/market-reports/today",
            NotificationCommandService.MARKET_REPORT_TOPIC,
            "market-report:2026-08-21",
            LocalDateTime.of(2026, 8, 21, 17, 0),
            LocalDateTime.of(2026, 8, 22, 16, 59, 59));

    assertEquals(21L, id);
    assertEquals(NotificationCommandService.MARKET_REPORT_TOPIC, notifications.campaign.getTopic());
    assertEquals(21L, outboxes.outbox.getCampaignId());
    assertEquals(null, outboxes.outbox.getNotificationId());
  }

  private static class RecordingNotificationMapper implements NotificationMapper {
    private NotificationVo notification;
    private NotificationCampaignVo campaign;

    @Override
    public int insertNotification(NotificationVo notification) {
      this.notification = notification;
      notification.setNotificationId(11L);
      return 1;
    }

    @Override
    public int insertCampaign(NotificationCampaignVo campaign) {
      this.campaign = campaign;
      campaign.setCampaignId(21L);
      return 1;
    }

    @Override public NotificationVo findNotificationById(long notificationId) { return null; }
    @Override public NotificationCampaignVo findCampaignById(long campaignId) { return null; }
    @Override public List<NotificationFeedRow> findFeed(long userId, LocalDateTime now, int offset, int size) { return List.of(); }
    @Override public long countFeed(long userId, LocalDateTime now) { return 0; }
    @Override public long countUnread(long userId, LocalDateTime now) { return 0; }
    @Override public int markNotificationRead(long notificationId, long userId, LocalDateTime readAt) { return 0; }
    @Override public int insertCampaignReceipt(long campaignId, long userId, LocalDateTime readAt) { return 0; }
    @Override public int markAllNotificationsRead(long userId, LocalDateTime readAt) { return 0; }
    @Override public int markAllCampaignsRead(long userId, LocalDateTime now, LocalDateTime readAt) { return 0; }
    @Override public int markNotificationProcessing(long notificationId, int expectedAttempts) { return 0; }
    @Override public int markCampaignProcessing(long campaignId, int expectedAttempts) { return 0; }
    @Override public int markNotificationSent(long notificationId, String status, LocalDateTime sentAt) { return 0; }
    @Override public int markCampaignSent(long campaignId, String status, LocalDateTime sentAt) { return 0; }
    @Override public int markNotificationFailed(long notificationId) { return 0; }
    @Override public int markCampaignFailed(long campaignId) { return 0; }
    @Override public List<Long> findDailyMissionTargetUserIds(long lastUserId, int limit) { return List.of(); }
    @Override public List<Long> findActiveInvestmentPlanUserIds(long lastUserId, int limit) { return List.of(); }
  }

  private static class RecordingOutboxMapper implements NotificationOutboxMapper {
    private NotificationOutboxVo outbox;

    @Override
    public int insert(NotificationOutboxVo outbox) {
      this.outbox = outbox;
      outbox.setOutboxId(31L);
      return 1;
    }

    @Override public NotificationOutboxVo findById(long outboxId) { return null; }
    @Override public List<NotificationOutboxVo> findClaimableForUpdate(LocalDateTime now, int limit, int maxAttempts) { return List.of(); }
    @Override public int markProcessing(long outboxId, LocalDateTime claimedAt) { return 0; }
    @Override public int markPublished(long outboxId, LocalDateTime publishedAt) { return 0; }
    @Override public int markRetry(long outboxId, LocalDateTime availableAt, String lastError, boolean terminal) { return 0; }
    @Override public int resetStaleProcessing(LocalDateTime staleBefore, LocalDateTime availableAt) { return 0; }
  }
}
