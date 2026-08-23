package com.jaedaero.domain.notification.vo;

import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.common.PushStatus;
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
public class NotificationVo {
  private Long notificationId;
  private Long userId;
  private NotificationType notificationType;
  private String title;
  private String body;
  private String deepLink;
  private String dedupeKey;
  private boolean read;
  private PushStatus pushStatus;
  private int pushAttempts;
  private LocalDateTime sentAt;
  private LocalDateTime readAt;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
