package com.jaedaero.domain.notification.service;

import com.jaedaero.domain.notification.dto.NotificationPageResponse;
import com.jaedaero.domain.notification.dto.UnreadCountResponse;

public interface NotificationService {
  NotificationPageResponse getNotifications(long userId, int page, int size);

  UnreadCountResponse getUnreadCount(long userId);

  void markRead(long userId, String notificationId);

  void markAllRead(long userId);
}
