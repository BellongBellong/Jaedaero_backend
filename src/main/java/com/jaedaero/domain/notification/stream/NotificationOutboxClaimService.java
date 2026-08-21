package com.jaedaero.domain.notification.stream;

import com.jaedaero.domain.notification.mapper.NotificationOutboxMapper;
import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationOutboxClaimService {
  private final NotificationOutboxMapper outboxMapper;
  private final Clock clock;

  public NotificationOutboxClaimService(NotificationOutboxMapper outboxMapper, Clock clock) {
    this.outboxMapper = outboxMapper;
    this.clock = clock;
  }

  @Transactional
  public List<NotificationOutboxVo> claim(int batchSize, int maxAttempts) {
    LocalDateTime now = LocalDateTime.now(clock);
    List<NotificationOutboxVo> outboxes =
        outboxMapper.findClaimableForUpdate(now, batchSize, maxAttempts);
    for (NotificationOutboxVo outbox : outboxes) {
      outboxMapper.markProcessing(outbox.getOutboxId(), now);
      outbox.setPublishAttempts(outbox.getPublishAttempts() + 1);
    }
    return outboxes;
  }

  @Transactional
  public void recoverStaleClaims(int staleSeconds) {
    LocalDateTime now = LocalDateTime.now(clock);
    outboxMapper.resetStaleProcessing(now.minusSeconds(staleSeconds), now);
  }
}
