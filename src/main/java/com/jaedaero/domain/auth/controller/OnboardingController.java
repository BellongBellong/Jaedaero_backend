package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.MilitaryInfoRequest;
import com.jaedaero.domain.auth.dto.SoldierProfileResponse;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.MilitaryInfoException;
import com.jaedaero.domain.auth.service.MilitaryInfoService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
public class OnboardingController {

  private final MilitaryInfoService militaryInfoService;

  @PostMapping("/military-info")
  public ResponseEntity<SoldierProfileResponse> registerMilitaryInfo(
      Authentication authentication, @Valid @RequestBody MilitaryInfoRequest request) {
    return ResponseEntity.ok(
        militaryInfoService.registerMilitaryInfo(getAuthenticatedUserId(authentication), request));
  }

  private long getAuthenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new MilitaryInfoException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }

    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new MilitaryInfoException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
  }
}
