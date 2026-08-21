package com.jaedaero.domain.notification.stream;

import com.jaedaero.domain.notification.mapper.NotificationMapper;
import com.jaedaero.domain.notification.mapper.NotificationOutboxMapper;
import com.jaedaero.domain.notification.push.NotificationPushGateway;
import com.jaedaero.domain.notification.push.PushDeliveryResult;
import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import com.jaedaero.domain.notification.vo.NotificationVo;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Range;
import org.springframework.data.redis.connection.stream.Consumer;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.PendingMessage;
import org.springframework.data.redis.connection.stream.PendingMessages;
import org.springframework.data.redis.connection.stream.ReadOffset;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamOffset;
import org.springframework.data.redis.connection.stream.StreamReadOptions;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationStreamConsumer {
  private final StringRedisTemplate redisTemplate;
  private final NotificationOutboxMapper outboxMapper;
  private final NotificationMapper notificationMapper;
  private final NotificationPushGateway pushGateway;
  private final Clock clock;
  private final String streamKey;
  private final String group;
  private final String consumerName;
  private final String dlqKey;
  private final boolean fcmEnabled;
  private final int batchSize;
  private final int maxDeliveryAttempts;
  private final Duration pendingMinIdle;

  public NotificationStreamConsumer(
      StringRedisTemplate redisTemplate,
      NotificationOutboxMapper outboxMapper,
      NotificationMapper notificationMapper,
      NotificationPushGateway pushGateway,
      Clock clock,
      @Value("${REDIS_STREAM_NOTIFICATION_KEY:stream:notification:v1}") String streamKey,
      @Value("${REDIS_STREAM_NOTIFICATION_GROUP:notification-fcm-group}") String group,
      @Value("${REDIS_STREAM_NOTIFICATION_DLQ_KEY:stream:notification:dlq:v1}") String dlqKey,
      @Value("${FCM_ENABLED:false}") boolean fcmEnabled,
      @Value("${NOTIFICATION_CONSUMER_BATCH_SIZE:50}") int batchSize,
      @Value("${NOTIFICATION_FCM_MAX_ATTEMPTS:5}") int maxDeliveryAttempts,
      @Value("${NOTIFICATION_PENDING_MIN_IDLE_MS:30000}") long pendingMinIdleMillis) {
    this.redisTemplate = redisTemplate;
    this.outboxMapper = outboxMapper;
    this.notificationMapper = notificationMapper;
    this.pushGateway = pushGateway;
    this.clock = clock;
    this.streamKey = streamKey;
    this.group = group;
    this.dlqKey = dlqKey;
    this.fcmEnabled = fcmEnabled;
    this.batchSize = batchSize;
    this.maxDeliveryAttempts = maxDeliveryAttempts;
    this.pendingMinIdle = Duration.ofMillis(pendingMinIdleMillis);
    String hostname = System.getenv("HOSTNAME");
    this.consumerName =
        "notification-" + (hostname == null || hostname.isBlank() ? "local" : hostname);
  }

  @PostConstruct
  public void initializeConsumerGroup() {
    if (!fcmEnabled) {
      log.info("FCM이 비활성화되어 Redis Stream Consumer Group 초기화를 건너뜁니다.");
      return;
    }
    ensureGroup();
  }

  @Scheduled(fixedDelayString = "${NOTIFICATION_CONSUMER_POLL_DELAY_MS:1000}")
  public void consumeNewMessages() {
    if (!fcmEnabled) {
      return;
    }
    try {
      List<MapRecord<String, Object, Object>> records =
          redisTemplate
              .opsForStream()
              .read(
                  Consumer.from(group, consumerName),
                  StreamReadOptions.empty().count(batchSize).block(Duration.ofSeconds(1)),
                  StreamOffset.create(streamKey, ReadOffset.lastConsumed()));
      process(records);
    } catch (RuntimeException exception) {
      if (isMissingGroup(exception)) {
        ensureGroup();
      } else {
        log.warn("알림 Redis Stream 소비에 실패했습니다. reason={}", safeMessage(exception));
      }
    }
  }

  @Scheduled(fixedDelayString = "${NOTIFICATION_PENDING_RECOVERY_DELAY_MS:30000}")
  public void recoverPendingMessages() {
    if (!fcmEnabled) {
      return;
    }
    try {
      PendingMessages pending =
          redisTemplate.opsForStream().pending(streamKey, group, Range.unbounded(), batchSize);
      List<RecordId> claimableIds =
          pending.stream()
              .filter(message -> message.getElapsedTimeSinceLastDelivery().compareTo(pendingMinIdle) >= 0)
              .map(PendingMessage::getId)
              .toList();
      if (claimableIds.isEmpty()) {
        return;
      }
      List<MapRecord<String, Object, Object>> claimed =
          redisTemplate
              .opsForStream()
              .claim(
                  streamKey,
                  group,
                  consumerName,
                  pendingMinIdle,
                  claimableIds.toArray(RecordId[]::new));
      process(claimed);
    } catch (RuntimeException exception) {
      if (isMissingGroup(exception)) {
        ensureGroup();
      } else {
        log.warn("알림 Pending 이벤트 회수에 실패했습니다. reason={}", safeMessage(exception));
      }
    }
  }

  private void process(List<MapRecord<String, Object, Object>> records) {
    if (records == null) {
      return;
    }
    for (MapRecord<String, Object, Object> record : records) {
      processOne(record);
    }
  }

  private void processOne(MapRecord<String, Object, Object> record) {
    Object rawOutboxId = record.getValue().get("outboxId");
    if (rawOutboxId == null) {
      moveToDlqAndAcknowledge(record, "outboxId 누락");
      return;
    }
    long outboxId;
    try {
      outboxId = Long.parseLong(String.valueOf(rawOutboxId));
    } catch (NumberFormatException exception) {
      moveToDlqAndAcknowledge(record, "outboxId 형식 오류");
      return;
    }

    NotificationOutboxVo outbox = outboxMapper.findById(outboxId);
    if (outbox == null) {
      moveToDlqAndAcknowledge(record, "Outbox 행 없음");
      return;
    }
    if (outbox.getNotificationId() != null) {
      processNotification(record, outbox);
    } else if (outbox.getCampaignId() != null) {
      processCampaign(record, outbox);
    } else {
      moveToDlqAndAcknowledge(record, "Outbox 대상 없음");
    }
  }

  private void processNotification(
      MapRecord<String, Object, Object> record, NotificationOutboxVo outbox) {
    NotificationVo notification =
        notificationMapper.findNotificationById(outbox.getNotificationId());
    if (notification == null) {
      moveToDlqAndAcknowledge(record, "개인 알림 행 없음");
      return;
    }
    if (isFinished(notification.getPushStatus().name())) {
      acknowledge(record);
      return;
    }
    if (notificationMapper.markNotificationProcessing(
            notification.getNotificationId(), notification.getPushAttempts())
        == 0) {
      acknowledge(record);
      return;
    }
    PushDeliveryResult result = pushGateway.send(notification);
    handleNotificationResult(record, notification, result);
  }

  private void processCampaign(
      MapRecord<String, Object, Object> record, NotificationOutboxVo outbox) {
    NotificationCampaignVo campaign = notificationMapper.findCampaignById(outbox.getCampaignId());
    if (campaign == null) {
      moveToDlqAndAcknowledge(record, "공통 알림 행 없음");
      return;
    }
    if (isFinished(campaign.getPushStatus().name())) {
      acknowledge(record);
      return;
    }
    if (notificationMapper.markCampaignProcessing(
            campaign.getCampaignId(), campaign.getPushAttempts())
        == 0) {
      acknowledge(record);
      return;
    }
    PushDeliveryResult result = pushGateway.send(campaign);
    handleCampaignResult(record, campaign, result);
  }

  private void handleNotificationResult(
      MapRecord<String, Object, Object> record,
      NotificationVo notification,
      PushDeliveryResult result) {
    int attempt = notification.getPushAttempts() + 1;
    if (result.status() == PushDeliveryResult.Status.SENT
        || result.status() == PushDeliveryResult.Status.SKIPPED) {
      notificationMapper.markNotificationSent(
          notification.getNotificationId(),
          result.status() == PushDeliveryResult.Status.SENT ? "SENT" : "SKIPPED",
          LocalDateTime.now(clock));
      acknowledge(record);
    } else if (result.status() == PushDeliveryResult.Status.PERMANENT_FAILURE
        || attempt >= maxDeliveryAttempts) {
      notificationMapper.markNotificationFailed(notification.getNotificationId());
      moveToDlqAndAcknowledge(record, result.detail());
    }
  }

  private void handleCampaignResult(
      MapRecord<String, Object, Object> record,
      NotificationCampaignVo campaign,
      PushDeliveryResult result) {
    int attempt = campaign.getPushAttempts() + 1;
    if (result.status() == PushDeliveryResult.Status.SENT
        || result.status() == PushDeliveryResult.Status.SKIPPED) {
      notificationMapper.markCampaignSent(
          campaign.getCampaignId(),
          result.status() == PushDeliveryResult.Status.SENT ? "SENT" : "SKIPPED",
          LocalDateTime.now(clock));
      acknowledge(record);
    } else if (result.status() == PushDeliveryResult.Status.PERMANENT_FAILURE
        || attempt >= maxDeliveryAttempts) {
      notificationMapper.markCampaignFailed(campaign.getCampaignId());
      moveToDlqAndAcknowledge(record, result.detail());
    }
  }

  private boolean isFinished(String status) {
    return "SENT".equals(status) || "SKIPPED".equals(status) || "FAILED".equals(status);
  }

  private void moveToDlqAndAcknowledge(MapRecord<String, Object, Object> record, String reason) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("sourceRecordId", record.getId().getValue());
    Object outboxId = record.getValue().get("outboxId");
    body.put("outboxId", outboxId == null ? "" : String.valueOf(outboxId));
    body.put("reason", truncate(reason == null ? "unknown" : reason));
    redisTemplate.opsForStream().add(StreamRecords.newRecord().in(dlqKey).ofMap(body));
    acknowledge(record);
  }

  private void acknowledge(MapRecord<String, Object, Object> record) {
    redisTemplate.opsForStream().acknowledge(streamKey, group, record.getId());
  }

  private void ensureGroup() {
    try {
      redisTemplate.opsForStream().createGroup(streamKey, ReadOffset.from("0-0"), group);
      log.info("알림 Redis Stream Consumer Group을 생성했습니다. stream={}, group={}", streamKey, group);
    } catch (DataAccessException exception) {
      if (!safeMessage(exception).contains("BUSYGROUP")) {
        log.warn("알림 Redis Stream Consumer Group 초기화에 실패했습니다. reason={}", safeMessage(exception));
      }
    }
  }

  private boolean isMissingGroup(Throwable throwable) {
    return safeMessage(throwable).contains("NOGROUP");
  }

  private String truncate(String value) {
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  private String safeMessage(Throwable throwable) {
    String message = throwable.getMessage();
    return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
  }
}
