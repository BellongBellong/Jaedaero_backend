package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.UserAgreementRequest;
import com.jaedaero.domain.auth.dto.UserAgreementResponse;
import com.jaedaero.domain.auth.exception.AgreementException;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.service.AgreementService;
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
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
public class AgreementController {

  private final AgreementService agreementService;

  @PostMapping
  public ResponseEntity<UserAgreementResponse> recordAgreements(
      Authentication authentication, @Valid @RequestBody UserAgreementRequest request) {
    return ResponseEntity.ok(
        agreementService.recordAgreements(getAuthenticatedUserId(authentication), request));
  }

  private long getAuthenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AgreementException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }

    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new AgreementException(AuthErrorCode.AUTHENTICATION_REQUIRED, "인증이 필요합니다.");
    }
  }
}
