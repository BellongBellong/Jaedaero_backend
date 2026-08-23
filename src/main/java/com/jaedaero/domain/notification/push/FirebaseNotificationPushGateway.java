package com.jaedaero.domain.notification.push;

import com.google.auth.oauth2.GoogleCredentials;
import com.google.firebase.FirebaseApp;
import com.google.firebase.FirebaseOptions;
import com.google.firebase.messaging.BatchResponse;
import com.google.firebase.messaging.FirebaseMessaging;
import com.google.firebase.messaging.FirebaseMessagingException;
import com.google.firebase.messaging.Message;
import com.google.firebase.messaging.MessagingErrorCode;
import com.google.firebase.messaging.MulticastMessage;
import com.google.firebase.messaging.SendResponse;
import com.jaedaero.domain.notification.mapper.DeviceTokenMapper;
import com.jaedaero.domain.notification.vo.DeviceTokenVo;
import com.jaedaero.domain.notification.vo.NotificationCampaignVo;
import com.jaedaero.domain.notification.vo.NotificationVo;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Slf4j
@Component
public class FirebaseNotificationPushGateway implements NotificationPushGateway {
  private static final String APP_NAME = "jaedaero-notification";

  private final DeviceTokenMapper tokenMapper;
  private final String projectId;
  private final boolean dryRun;
  private volatile FirebaseMessaging firebaseMessaging;

  public FirebaseNotificationPushGateway(
      DeviceTokenMapper tokenMapper,
      @Value("${FIREBASE_PROJECT_ID:}") String projectId,
      @Value("${FCM_DRY_RUN:true}") boolean dryRun) {
    this.tokenMapper = tokenMapper;
    this.projectId = projectId;
    this.dryRun = dryRun;
  }

  @Override
  public PushDeliveryResult send(NotificationVo notification) {
    List<DeviceTokenVo> tokens = tokenMapper.findActiveByUserId(notification.getUserId());
    if (tokens.isEmpty()) {
      return PushDeliveryResult.skipped("활성 FCM 토큰 없음");
    }
    List<String> values = tokens.stream().map(DeviceTokenVo::getFcmToken).toList();
    MulticastMessage.Builder builder =
        MulticastMessage.builder()
            .addAllTokens(values)
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(notification.getTitle())
                    .setBody(notification.getBody())
                    .build())
            .putData("notificationId", "N" + notification.getNotificationId())
            .putData("type", notification.getNotificationType().name());
    putDeepLink(builder, notification.getDeepLink());
    try {
      BatchResponse response = messaging().sendEachForMulticast(builder.build(), dryRun);
      return evaluate(response, values);
    } catch (FirebaseMessagingException exception) {
      return fromException(exception);
    } catch (RuntimeException exception) {
      return PushDeliveryResult.retryable(safeMessage(exception));
    }
  }

  @Override
  public PushDeliveryResult send(NotificationCampaignVo campaign) {
    Message.Builder builder =
        Message.builder()
            .setTopic(campaign.getTopic())
            .setNotification(
                com.google.firebase.messaging.Notification.builder()
                    .setTitle(campaign.getTitle())
                    .setBody(campaign.getBody())
                    .build())
            .putData("notificationId", "C" + campaign.getCampaignId())
            .putData("type", campaign.getNotificationType().name());
    if (StringUtils.hasText(campaign.getDeepLink())) {
      builder.putData("deepLink", campaign.getDeepLink());
    }
    try {
      messaging().send(builder.build(), dryRun);
      return PushDeliveryResult.sent();
    } catch (FirebaseMessagingException exception) {
      return fromException(exception);
    } catch (RuntimeException exception) {
      return PushDeliveryResult.retryable(safeMessage(exception));
    }
  }

  @Override
  public void subscribeToTopic(String fcmToken, String topic) {
    try {
      messaging().subscribeToTopic(List.of(fcmToken), topic);
    } catch (FirebaseMessagingException exception) {
      throw new IllegalStateException("FCM 토픽 구독에 실패했습니다.", exception);
    }
  }

  private void putDeepLink(MulticastMessage.Builder builder, String deepLink) {
    if (StringUtils.hasText(deepLink)) {
      builder.putData("deepLink", deepLink);
    }
  }

  private PushDeliveryResult evaluate(BatchResponse response, List<String> tokens) {
    if (response.getFailureCount() == 0) {
      return PushDeliveryResult.sent();
    }
    boolean retryableFailure = false;
    List<String> permanentErrors = new ArrayList<>();
    List<SendResponse> responses = response.getResponses();
    for (int index = 0; index < responses.size(); index++) {
      SendResponse sendResponse = responses.get(index);
      if (sendResponse.isSuccessful()) {
        continue;
      }
      FirebaseMessagingException exception = sendResponse.getException();
      MessagingErrorCode code = exception == null ? null : exception.getMessagingErrorCode();
      if (code == MessagingErrorCode.UNREGISTERED
          || code == MessagingErrorCode.SENDER_ID_MISMATCH) {
        tokenMapper.deactivateByFcmToken(tokens.get(index));
        permanentErrors.add(String.valueOf(code));
      } else if (isRetryable(code)) {
        retryableFailure = true;
      } else {
        permanentErrors.add(String.valueOf(code));
      }
    }
    if (retryableFailure) {
      return PushDeliveryResult.retryable("일부 FCM 토큰 발송 일시 실패");
    }
    if (response.getSuccessCount() > 0) {
      return PushDeliveryResult.sent();
    }
    return PushDeliveryResult.permanent(String.join(",", permanentErrors));
  }

  private PushDeliveryResult fromException(FirebaseMessagingException exception) {
    MessagingErrorCode code = exception.getMessagingErrorCode();
    return isRetryable(code)
        ? PushDeliveryResult.retryable(code + ": " + safeMessage(exception))
        : PushDeliveryResult.permanent(code + ": " + safeMessage(exception));
  }

  private boolean isRetryable(MessagingErrorCode code) {
    return code == MessagingErrorCode.INTERNAL
        || code == MessagingErrorCode.UNAVAILABLE
        || code == MessagingErrorCode.QUOTA_EXCEEDED;
  }

  private FirebaseMessaging messaging() {
    FirebaseMessaging local = firebaseMessaging;
    if (local != null) {
      return local;
    }
    synchronized (this) {
      if (firebaseMessaging == null) {
        if (!StringUtils.hasText(projectId)) {
          throw new IllegalStateException("FIREBASE_PROJECT_ID가 설정되지 않았습니다.");
        }
        try {
          FirebaseOptions options =
              FirebaseOptions.builder()
                  .setCredentials(GoogleCredentials.getApplicationDefault())
                  .setProjectId(projectId)
                  .build();
          FirebaseApp app =
              FirebaseApp.getApps().stream()
                  .filter(candidate -> APP_NAME.equals(candidate.getName()))
                  .findFirst()
                  .orElseGet(() -> FirebaseApp.initializeApp(options, APP_NAME));
          firebaseMessaging = FirebaseMessaging.getInstance(app);
          log.info("Firebase Admin SDK 초기화가 완료되었습니다. projectId={}", projectId);
        } catch (IOException exception) {
          throw new IllegalStateException("Firebase 서비스 계정 인증 정보를 읽지 못했습니다.", exception);
        }
      }
      return firebaseMessaging;
    }
  }

  private String safeMessage(Throwable throwable) {
    String message = throwable.getMessage();
    return message == null || message.isBlank() ? throwable.getClass().getSimpleName() : message;
  }
}
