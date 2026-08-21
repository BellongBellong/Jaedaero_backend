package com.jaedaero.domain.notification.dto;

import com.jaedaero.domain.notification.common.DeviceType;
import com.jaedaero.domain.notification.vo.DeviceTokenVo;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class DeviceTokenResponse {
  private final long deviceTokenId;
  private final DeviceType platform;
  private final boolean active;

  public static DeviceTokenResponse from(DeviceTokenVo token) {
    return DeviceTokenResponse.builder()
        .deviceTokenId(token.getDeviceTokenId())
        .platform(token.getDeviceType())
        .active(token.isActive())
        .build();
  }
}
