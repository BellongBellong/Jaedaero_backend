package com.jaedaero.domain.notification.controller;

import com.jaedaero.domain.notification.dto.DeviceTokenRequest;
import com.jaedaero.domain.notification.dto.DeviceTokenResponse;
import com.jaedaero.domain.notification.dto.NotificationPageResponse;
import com.jaedaero.domain.notification.dto.UnreadCountResponse;
import com.jaedaero.domain.notification.exception.NotificationErrorCode;
import com.jaedaero.domain.notification.exception.NotificationException;
import com.jaedaero.domain.notification.service.DeviceTokenService;
import com.jaedaero.domain.notification.service.NotificationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@Api(tags = "알림")
@Validated
@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class NotificationController {
  private final DeviceTokenService deviceTokenService;
  private final NotificationService notificationService;

  @PostMapping("/device-tokens")
  @ApiOperation(value = "FCM 디바이스 토큰 등록 또는 갱신")
  public ResponseEntity<DeviceTokenResponse> registerDeviceToken(
      @ApiIgnore Authentication authentication, @Valid @RequestBody DeviceTokenRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(deviceTokenService.register(authenticatedUserId(authentication), request));
  }

  @DeleteMapping("/device-tokens/{deviceTokenId}")
  @ApiOperation(value = "FCM 디바이스 토큰 비활성화")
  public ResponseEntity<Void> deactivateDeviceToken(
      @ApiIgnore Authentication authentication, @PathVariable long deviceTokenId) {
    deviceTokenService.deactivate(authenticatedUserId(authentication), deviceTokenId);
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/notifications")
  @ApiOperation(value = "내 알림 목록 조회")
  public ResponseEntity<NotificationPageResponse> getNotifications(
      @ApiIgnore Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(
        notificationService.getNotifications(authenticatedUserId(authentication), page, size));
  }

  @GetMapping("/notifications/unread-count")
  @ApiOperation(value = "읽지 않은 알림 개수 조회")
  public ResponseEntity<UnreadCountResponse> getUnreadCount(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(
        notificationService.getUnreadCount(authenticatedUserId(authentication)));
  }

  @PutMapping("/notifications/{notificationId}/read")
  @ApiOperation(value = "알림 읽음 처리", notes = "개인 알림은 N{id}, 공통 알림은 C{id} 형식을 사용합니다.")
  public ResponseEntity<Void> markRead(
      @ApiIgnore Authentication authentication, @PathVariable String notificationId) {
    notificationService.markRead(authenticatedUserId(authentication), notificationId);
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/notifications/read-all")
  @ApiOperation(value = "내 알림 전체 읽음 처리")
  public ResponseEntity<Void> markAllRead(@ApiIgnore Authentication authentication) {
    notificationService.markAllRead(authenticatedUserId(authentication));
    return ResponseEntity.noContent().build();
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new NotificationException(
          NotificationErrorCode.AUTHENTICATION_REQUIRED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new NotificationException(
          NotificationErrorCode.AUTHENTICATION_REQUIRED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
