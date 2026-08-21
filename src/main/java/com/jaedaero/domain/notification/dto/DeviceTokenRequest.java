package com.jaedaero.domain.notification.dto;

import com.jaedaero.domain.notification.common.DeviceType;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class DeviceTokenRequest {
  @NotBlank(message = "FCM 토큰은 필수입니다.")
  @Size(max = 500, message = "FCM 토큰은 500자를 초과할 수 없습니다.")
  private String token;

  @NotNull(message = "디바이스 유형은 필수입니다.")
  private DeviceType platform;
}
