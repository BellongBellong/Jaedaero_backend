package com.jaedaero.domain.notification.vo;

import com.jaedaero.domain.notification.common.OutboxStatus;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class NotificationOutboxVo {
  private Long outboxId;
  private String eventId;
  private Long notificationId;
  private Long campaignId;
  private String dedupeKey;
  private OutboxStatus status;
  private int publishAttempts;
  private LocalDateTime availableAt;
  private LocalDateTime claimedAt;
  private LocalDateTime publishedAt;
  private String lastError;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
