package com.jaedaero.domain.notification.mapper;

import com.jaedaero.domain.notification.vo.NotificationOutboxVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationOutboxMapper {
  int insert(NotificationOutboxVo outbox);

  NotificationOutboxVo findById(@Param("outboxId") long outboxId);

  List<NotificationOutboxVo> findClaimableForUpdate(
      @Param("now") LocalDateTime now,
      @Param("limit") int limit,
      @Param("maxAttempts") int maxAttempts);

  int markProcessing(
      @Param("outboxId") long outboxId, @Param("claimedAt") LocalDateTime claimedAt);

  int markPublished(
      @Param("outboxId") long outboxId, @Param("publishedAt") LocalDateTime publishedAt);

  int markRetry(
      @Param("outboxId") long outboxId,
      @Param("availableAt") LocalDateTime availableAt,
      @Param("lastError") String lastError,
      @Param("terminal") boolean terminal);

  int resetStaleProcessing(
      @Param("staleBefore") LocalDateTime staleBefore,
      @Param("availableAt") LocalDateTime availableAt);
}
