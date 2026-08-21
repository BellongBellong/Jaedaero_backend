package com.jaedaero.domain.notification.vo;

import com.jaedaero.domain.notification.common.DeviceType;
import java.time.LocalDateTime;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DeviceTokenVo {
  private Long deviceTokenId;
  private Long userId;
  private String fcmToken;
  private DeviceType deviceType;
  private boolean active;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
