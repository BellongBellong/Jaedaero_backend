package com.jaedaero.domain.notification.service.impl;

import com.jaedaero.domain.notification.dto.NotificationItemResponse;
import com.jaedaero.domain.notification.dto.NotificationPageResponse;
import com.jaedaero.domain.notification.dto.UnreadCountResponse;
import com.jaedaero.domain.notification.exception.NotificationErrorCode;
import com.jaedaero.domain.notification.exception.NotificationException;
import com.jaedaero.domain.notification.mapper.NotificationMapper;
import com.jaedaero.domain.notification.service.NotificationService;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class NotificationServiceImpl implements NotificationService {
  private final NotificationMapper notificationMapper;
  private final Clock clock;

  public NotificationServiceImpl(NotificationMapper notificationMapper, Clock clock) {
    this.notificationMapper = notificationMapper;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public NotificationPageResponse getNotifications(long userId, int page, int size) {
    LocalDateTime now = LocalDateTime.now(clock);
    long total = notificationMapper.countFeed(userId, now);
    List<NotificationItemResponse> items =
        notificationMapper.findFeed(userId, now, page * size, size).stream()
            .map(NotificationItemResponse::from)
            .toList();
    return NotificationPageResponse.builder()
        .items(items)
        .page(page)
        .size(size)
        .totalElements(total)
        .hasNext((long) (page + 1) * size < total)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public UnreadCountResponse getUnreadCount(long userId) {
    return new UnreadCountResponse(
        notificationMapper.countUnread(userId, LocalDateTime.now(clock)));
  }

  @Override
  @Transactional
  public void markRead(long userId, String notificationId) {
    ParsedNotificationId parsed = parse(notificationId);
    LocalDateTime now = LocalDateTime.now(clock);
    int updated =
        parsed.campaign
            ? notificationMapper.insertCampaignReceipt(parsed.id, userId, now)
            : notificationMapper.markNotificationRead(parsed.id, userId, now);
    if (updated == 0) {
      throw new NotificationException(
          NotificationErrorCode.NOTIFICATION_NOT_FOUND, "읽음 처리할 알림을 찾을 수 없습니다.");
    }
  }

  @Override
  @Transactional
  public void markAllRead(long userId) {
    LocalDateTime now = LocalDateTime.now(clock);
    notificationMapper.markAllNotificationsRead(userId, now);
    notificationMapper.markAllCampaignsRead(userId, now, now);
  }

  private ParsedNotificationId parse(String value) {
    if (value == null || value.length() < 2) {
      throw invalidId();
    }
    char prefix = value.charAt(0);
    if (prefix != 'N' && prefix != 'C') {
      throw invalidId();
    }
    try {
      long id = Long.parseLong(value.substring(1));
      if (id <= 0) {
        throw invalidId();
      }
      return new ParsedNotificationId(prefix == 'C', id);
    } catch (NumberFormatException exception) {
      throw invalidId();
    }
  }

  private NotificationException invalidId() {
    return new NotificationException(
        NotificationErrorCode.INVALID_NOTIFICATION_ID,
        "알림 ID는 N{숫자} 또는 C{숫자} 형식이어야 합니다.");
  }

  private record ParsedNotificationId(boolean campaign, long id) {}
}
