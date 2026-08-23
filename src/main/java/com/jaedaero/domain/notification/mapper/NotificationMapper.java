package com.jaedaero.domain.notification.mapper;

import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationFeedRow;
import com.jaedaero.domain.notification.vo.NotificationVo;
import java.time.LocalDateTime;
import java.util.List;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface NotificationMapper {
  int insertNotification(NotificationVo notification);

  int insertCampaign(NotificationCampaignVo campaign);

  NotificationVo findNotificationById(@Param("notificationId") long notificationId);

  NotificationCampaignVo findCampaignById(@Param("campaignId") long campaignId);

  List<NotificationFeedRow> findFeed(
      @Param("userId") long userId,
      @Param("now") LocalDateTime now,
      @Param("offset") int offset,
      @Param("size") int size);

  long countFeed(@Param("userId") long userId, @Param("now") LocalDateTime now);

  long countUnread(@Param("userId") long userId, @Param("now") LocalDateTime now);

  int markNotificationRead(
      @Param("notificationId") long notificationId,
      @Param("userId") long userId,
      @Param("readAt") LocalDateTime readAt);

  int insertCampaignReceipt(
      @Param("campaignId") long campaignId,
      @Param("userId") long userId,
      @Param("readAt") LocalDateTime readAt);

  int markAllNotificationsRead(
      @Param("userId") long userId, @Param("readAt") LocalDateTime readAt);

  int markAllCampaignsRead(
      @Param("userId") long userId,
      @Param("now") LocalDateTime now,
      @Param("readAt") LocalDateTime readAt);

  int markNotificationProcessing(
      @Param("notificationId") long notificationId,
      @Param("expectedAttempts") int expectedAttempts);

  int markCampaignProcessing(
      @Param("campaignId") long campaignId,
      @Param("expectedAttempts") int expectedAttempts);

  int markNotificationSent(
      @Param("notificationId") long notificationId,
      @Param("status") String status,
      @Param("sentAt") LocalDateTime sentAt);

  int markCampaignSent(
      @Param("campaignId") long campaignId,
      @Param("status") String status,
      @Param("sentAt") LocalDateTime sentAt);

  int markNotificationFailed(@Param("notificationId") long notificationId);

  int markCampaignFailed(@Param("campaignId") long campaignId);

  List<Long> findDailyMissionTargetUserIds(
      @Param("lastUserId") long lastUserId, @Param("limit") int limit);

  List<Long> findActiveInvestmentPlanUserIds(
      @Param("lastUserId") long lastUserId, @Param("limit") int limit);
}
