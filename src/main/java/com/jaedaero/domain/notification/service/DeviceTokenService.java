package com.jaedaero.domain.notification.service;

import com.jaedaero.domain.notification.dto.DeviceTokenRequest;
import com.jaedaero.domain.notification.dto.DeviceTokenResponse;

public interface DeviceTokenService {
  DeviceTokenResponse register(long userId, DeviceTokenRequest request);

  void deactivate(long userId, long deviceTokenId);
}
