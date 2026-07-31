package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.NicknameAvailabilityResponse;
import com.jaedaero.domain.auth.dto.NicknameRequest;
import com.jaedaero.domain.auth.dto.ProfileAppearanceRequest;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.NicknameException;
import com.jaedaero.domain.auth.service.NicknameService;
import com.jaedaero.domain.auth.service.ProfileAppearanceService;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
@Validated
public class UserController {

  private final NicknameService nicknameService;
  private final ProfileAppearanceService profileAppearanceService;

  @GetMapping("/nickname/availability")
  public ResponseEntity<NicknameAvailabilityResponse> checkNicknameAvailability(
      Authentication authentication,
      @RequestParam @NotBlank @Size(max = 12) @Pattern(regexp = "[가-힣a-zA-Z]+")
          String nickname) {
    long userId = getAuthenticatedUserId(authentication);
    return ResponseEntity.ok(
        new NicknameAvailabilityResponse(nicknameService.isAvailable(userId, nickname)));
  }

  @PutMapping("/nickname")
  public ResponseEntity<Void> updateNickname(
      Authentication authentication, @Valid @RequestBody NicknameRequest request) {
    nicknameService.updateNickname(getAuthenticatedUserId(authentication), request.getNickname());
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/profile-appearance")
  public ResponseEntity<Void> updateProfileAppearance(
      Authentication authentication, @Valid @RequestBody ProfileAppearanceRequest request) {
    profileAppearanceService.updateProfileAppearance(
        getAuthenticatedUserId(authentication), request.getProfileImage(), request.getProfileSource());
    return ResponseEntity.noContent().build();
  }

  private long getAuthenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new NicknameException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }

    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new NicknameException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
  }
}
