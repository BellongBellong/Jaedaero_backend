package com.jaedaero.domain.notification.dto;

import com.jaedaero.domain.notification.common.NotificationType;
import com.jaedaero.domain.notification.vo.NotificationFeedRow;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationItemResponse {
  private final String notificationId;
  private final NotificationType notificationType;
  private final String title;
  private final String body;
  private final String deepLink;
  private final boolean read;
  private final LocalDateTime createdAt;

  public static NotificationItemResponse from(NotificationFeedRow row) {
    return NotificationItemResponse.builder()
        .notificationId(row.getNotificationKey())
        .notificationType(row.getNotificationType())
        .title(row.getTitle())
        .body(row.getBody())
        .deepLink(row.getDeepLink())
        .read(row.isRead())
        .createdAt(row.getCreatedAt())
        .build();
  }
}
