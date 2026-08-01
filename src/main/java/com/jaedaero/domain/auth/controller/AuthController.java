package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.LoginRequest;
import com.jaedaero.domain.auth.dto.LoginResponse;
import com.jaedaero.domain.auth.dto.RefreshTokenRequest;
import com.jaedaero.domain.auth.dto.RefreshTokenResponse;
import com.jaedaero.domain.auth.service.SocialLoginService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Api(tags = "인증")
public class AuthController {

  private final SocialLoginService socialLoginService;

  @PostMapping("/login")
  @ApiOperation(value = "소셜 로그인", notes = "소셜 인가 코드로 로그인하고 JWT를 발급합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "로그인 성공", response = LoginResponse.class),
    @ApiResponse(code = 400, message = "지원하지 않는 소셜 로그인 유형 또는 잘못된 요청"),
    @ApiResponse(code = 401, message = "소셜 인증 실패"),
    @ApiResponse(code = 403, message = "탈퇴한 계정")
  })
  public ResponseEntity<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
    return ResponseEntity.ok(socialLoginService.login(request));
  }

  @PostMapping("/refresh")
  @ApiOperation(value = "토큰 재발급", notes = "리프레시 토큰으로 새 액세스 토큰을 발급합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "재발급 성공", response = RefreshTokenResponse.class),
    @ApiResponse(code = 401, message = "유효하지 않거나 만료된 리프레시 토큰")
  })
  public ResponseEntity<RefreshTokenResponse> refresh(
      @Valid @RequestBody RefreshTokenRequest request) {
    return ResponseEntity.ok(socialLoginService.refresh(request));
  }

  @PostMapping("/logout")
  @ApiOperation(value = "로그아웃", notes = "현재 사용자의 리프레시 토큰을 삭제합니다.")
  @ApiResponse(code = 204, message = "로그아웃 성공")
  public ResponseEntity<Void> logout(@ApiIgnore Authentication authentication) {
    if (authentication != null
        && authentication.isAuthenticated()
        && !(authentication instanceof AnonymousAuthenticationToken)) {
      socialLoginService.logout(Long.parseLong(authentication.getName()));
    }
    return ResponseEntity.noContent().build();
  }
}
