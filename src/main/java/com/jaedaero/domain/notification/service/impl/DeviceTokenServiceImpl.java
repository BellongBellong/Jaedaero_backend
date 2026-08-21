package com.jaedaero.domain.notification.service.impl;

import com.jaedaero.domain.notification.dto.DeviceTokenRequest;
import com.jaedaero.domain.notification.dto.DeviceTokenResponse;
import com.jaedaero.domain.notification.exception.NotificationErrorCode;
import com.jaedaero.domain.notification.exception.NotificationException;
import com.jaedaero.domain.notification.mapper.DeviceTokenMapper;
import com.jaedaero.domain.notification.push.NotificationPushGateway;
import com.jaedaero.domain.notification.service.NotificationCommandService;
import com.jaedaero.domain.notification.service.DeviceTokenService;
import com.jaedaero.domain.notification.vo.DeviceTokenVo;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class DeviceTokenServiceImpl implements DeviceTokenService {
  private final DeviceTokenMapper tokenMapper;
  private final NotificationPushGateway pushGateway;
  private final boolean fcmEnabled;

  @Autowired
  public DeviceTokenServiceImpl(
      DeviceTokenMapper tokenMapper,
      NotificationPushGateway pushGateway,
      @Value("${FCM_ENABLED:false}") boolean fcmEnabled) {
    this.tokenMapper = tokenMapper;
    this.pushGateway = pushGateway;
    this.fcmEnabled = fcmEnabled;
  }

  /** 단위 테스트와 FCM 미사용 환경을 위한 생성자입니다. */
  public DeviceTokenServiceImpl(DeviceTokenMapper tokenMapper) {
    this.tokenMapper = tokenMapper;
    this.pushGateway = null;
    this.fcmEnabled = false;
  }

  @Override
  @Transactional
  public DeviceTokenResponse register(long userId, DeviceTokenRequest request) {
    String normalizedToken = request.getToken().trim();
    DeviceTokenVo token =
        DeviceTokenVo.builder()
            .userId(userId)
            .fcmToken(normalizedToken)
            .deviceType(request.getPlatform())
            .active(true)
            .build();
    tokenMapper.upsert(token);
    DeviceTokenVo saved = tokenMapper.findByFcmToken(normalizedToken);
    if (saved == null) {
      throw new IllegalStateException("저장된 FCM 토큰을 확인할 수 없습니다.");
    }
    subscribeToMarketReportTopic(normalizedToken);
    return DeviceTokenResponse.from(saved);
  }

  @Override
  @Transactional
  public void deactivate(long userId, long deviceTokenId) {
    if (tokenMapper.deactivateByIdAndUserId(deviceTokenId, userId) == 0) {
      throw new NotificationException(
          NotificationErrorCode.TOKEN_NOT_FOUND, "비활성화할 디바이스 토큰을 찾을 수 없습니다.");
    }
  }

  private void subscribeToMarketReportTopic(String fcmToken) {
    if (!fcmEnabled || pushGateway == null) {
      return;
    }
    try {
      pushGateway.subscribeToTopic(fcmToken, NotificationCommandService.MARKET_REPORT_TOPIC);
    } catch (RuntimeException exception) {
      log.warn("시장 리포트 FCM 토픽 구독에 실패했습니다. tokenId는 저장되었습니다.", exception);
    }
  }
}
