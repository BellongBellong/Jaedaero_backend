package com.jaedaero.domain.notification.stream;

import com.jaedaero.domain.notification.mapper.NotificationOutboxMapper;
import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.redis.connection.stream.MapRecord;
import org.springframework.data.redis.connection.stream.RecordId;
import org.springframework.data.redis.connection.stream.StreamRecords;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NotificationOutboxPublisher {
  private final NotificationOutboxClaimService claimService;
  private final NotificationOutboxMapper outboxMapper;
  private final StringRedisTemplate redisTemplate;
  private final Clock clock;
  private final String streamKey;
  private final int batchSize;
  private final int maxAttempts;

  public NotificationOutboxPublisher(
      NotificationOutboxClaimService claimService,
      NotificationOutboxMapper outboxMapper,
      StringRedisTemplate redisTemplate,
      Clock clock,
      @Value("${REDIS_STREAM_NOTIFICATION_KEY:stream:notification:v1}") String streamKey,
      @Value("${NOTIFICATION_OUTBOX_BATCH_SIZE:100}") int batchSize,
      @Value("${NOTIFICATION_OUTBOX_MAX_ATTEMPTS:5}") int maxAttempts) {
    this.claimService = claimService;
    this.outboxMapper = outboxMapper;
    this.redisTemplate = redisTemplate;
    this.clock = clock;
    this.streamKey = streamKey;
    this.batchSize = batchSize;
    this.maxAttempts = maxAttempts;
  }

  @Scheduled(fixedDelayString = "${NOTIFICATION_OUTBOX_POLL_DELAY_MS:1000}")
  public void publishPending() {
    List<NotificationOutboxVo> claimed;
    try {
      claimed = claimService.claim(batchSize, maxAttempts);
    } catch (RuntimeException exception) {
      log.warn("알림 Outbox claim에 실패했습니다. reason={}", safeMessage(exception));
      return;
    }
    for (NotificationOutboxVo outbox : claimed) {
      publishOne(outbox);
    }
  }

  @Scheduled(fixedDelayString = "${NOTIFICATION_OUTBOX_RECOVERY_DELAY_MS:60000}")
  public void recoverStaleClaims() {
    try {
      claimService.recoverStaleClaims(60);
    } catch (RuntimeException exception) {
      log.warn("알림 Outbox stale claim 복구에 실패했습니다. reason={}", safeMessage(exception));
    }
  }

  private void publishOne(NotificationOutboxVo outbox) {
    Map<String, String> body = new LinkedHashMap<>();
    body.put("outboxId", String.valueOf(outbox.getOutboxId()));
    body.put("eventId", outbox.getEventId());
    MapRecord<String, String, String> record = StreamRecords.newRecord().in(streamKey).ofMap(body);
    try {
      RecordId recordId = redisTemplate.opsForStream().add(record);
      if (recordId == null) {
        throw new IllegalStateException("Redis XADD가 record ID를 반환하지 않았습니다.");
      }
      outboxMapper.markPublished(outbox.getOutboxId(), LocalDateTime.now(clock));
    } catch (RuntimeException exception) {
      boolean terminal = outbox.getPublishAttempts() >= maxAttempts;
      long backoffSeconds = Math.min(300L, 1L << Math.min(8, outbox.getPublishAttempts()));
      outboxMapper.markRetry(
          outbox.getOutboxId(),
          LocalDateTime.now(clock).plusSeconds(backoffSeconds),
          truncate(safeMessage(exception)),
          terminal);
      log.warn(
          "알림 Outbox Redis 발행에 실패했습니다. outboxId={}, attempt={}, terminal={}, reason={}",
          outbox.getOutboxId(),
          outbox.getPublishAttempts(),
          terminal,
          safeMessage(exception));
    }
  }

  private String truncate(String value) {
    return value.length() <= 500 ? value : value.substring(0, 500);
  }

  private String safeMessage(Throwable throwable) {
    String message = throwable.getMessage();
    return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
  }
}
