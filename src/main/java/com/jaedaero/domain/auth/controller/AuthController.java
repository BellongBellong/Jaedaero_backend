package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.LoginRequest;
import com.jaedaero.domain.auth.dto.LoginResponse;
import com.jaedaero.domain.auth.dto.RefreshTokenRequest;
import com.jaedaero.domain.auth.dto.RefreshTokenResponse;
import com.jaedaero.domain.auth.service.SocialLoginService;
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
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

  private final SocialLoginService socialLoginService;

  @PostMapping("/login")
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(socialLoginService.login(request));
  }

  @PostMapping("/refresh")
  public ResponseEntity<RefreshTokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(socialLoginService.refresh(request));
  }

  @PostMapping("/logout")
  public ResponseEntity<Void> logout(Authentication authentication) {
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      socialLoginService.logout(Long.parseLong(authentication.getName()));
    }
    return ResponseEntity.noContent().build();
  }
}
