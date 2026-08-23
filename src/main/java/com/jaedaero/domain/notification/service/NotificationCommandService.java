package com.jaedaero.domain.notification.service;

import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.mapper.NotificationMapper;
import com.jaedaero.domain.notification.mapper.NotificationOutboxMapper;
import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import com.jaedaero.domain.notification.vo.NotificationVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationCommandService {
  public static final String MARKET_REPORT_TOPIC = "market-report-arrived-v1";

  private final NotificationMapper notificationMapper;
  private final NotificationOutboxMapper outboxMapper;
  private final Clock clock;

  public NotificationCommandService(
      NotificationMapper notificationMapper, NotificationOutboxMapper outboxMapper, Clock clock) {
    this.notificationMapper = notificationMapper;
    this.outboxMapper = outboxMapper;
    this.clock = clock;
  }

  @Transactional
  public long createUserNotification(
      long userId,
      NotificationType type,
      String title,
      String body,
      String deepLink,
      String dedupeKey) {
    NotificationVo notification =
        NotificationVo.builder()
            .userId(userId)
            .notificationType(type)
            .title(title)
            .body(body)
            .deepLink(deepLink)
            .dedupeKey(dedupeKey)
            .build();
    notificationMapper.insertNotification(notification);
    if (notification.getNotificationId() == null) {
      throw new IllegalStateException("생성된 개인 알림 ID를 확인할 수 없습니다.");
    }
    insertOutbox(notification.getNotificationId(), null, "outbox:" + dedupeKey);
    return notification.getNotificationId();
  }

  @Transactional
  public long createCampaign(
      NotificationType type,
      String title,
      String body,
      String deepLink,
      String topic,
      String dedupeKey,
      LocalDateTime visibleFrom,
      LocalDateTime visibleUntil) {
    NotificationCampaignVo campaign =
        NotificationCampaignVo.builder()
            .notificationType(type)
            .title(title)
            .body(body)
            .deepLink(deepLink)
            .topic(topic)
            .dedupeKey(dedupeKey)
            .visibleFrom(visibleFrom)
            .visibleUntil(visibleUntil)
            .build();
    notificationMapper.insertCampaign(campaign);
    if (campaign.getCampaignId() == null) {
      throw new IllegalStateException("생성된 공통 알림 ID를 확인할 수 없습니다.");
    }
    insertOutbox(null, campaign.getCampaignId(), "outbox:" + dedupeKey);
    return campaign.getCampaignId();
  }

  private void insertOutbox(Long notificationId, Long campaignId, String dedupeKey) {
    outboxMapper.insert(
        NotificationOutboxVo.builder()
            .eventId(UUID.randomUUID().toString())
            .notificationId(notificationId)
            .campaignId(campaignId)
            .dedupeKey(dedupeKey)
            .availableAt(LocalDateTime.now(clock))
            .build());
  }
}
