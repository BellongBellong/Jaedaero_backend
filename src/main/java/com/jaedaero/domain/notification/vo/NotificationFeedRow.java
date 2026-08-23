package com.jaedaero.domain.notification.vo;

import com.jaedaero.domain.notification.common.NotificationType;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class NotificationFeedRow {
  private String notificationKey;
  private NotificationType notificationType;
  private String title;
  private String body;
  private String deepLink;
  private boolean read;
  private LocalDateTime createdAt;
}
