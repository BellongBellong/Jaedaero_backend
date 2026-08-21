package com.jaedaero.domain.notification.push;

import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationVo;

public interface NotificationPushGateway {
  PushDeliveryResult send(NotificationVo notification);

  PushDeliveryResult send(NotificationCampaignVo campaign);

  default void subscribeToTopic(String fcmToken, String topic) {
    // FCM 이외의 테스트 게이트웨이는 토픽 구독이 필요하지 않습니다.
  }
}
