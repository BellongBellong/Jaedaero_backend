package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.UserAgreementRequest;
import com.jaedaero.domain.auth.dto.UserAgreementResponse;
import com.jaedaero.domain.auth.exception.AgreementException;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.service.AgreementService;
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
@RequestMapping("/api/v1/agreements")
@RequiredArgsConstructor
@Api(tags = "회원가입")
public class AgreementController {

  private final AgreementService agreementService;

  @PostMapping
  @ApiOperation(value = "약관 동의 기록", notes = "필수 약관 동의 여부를 확인하고 동의 이력을 저장합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "저장 성공", response = UserAgreementResponse.class),
    @ApiResponse(code = 400, message = "필수 약관 미동의 또는 잘못된 요청"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<UserAgreementResponse> recordAgreements(
      @ApiIgnore Authentication authentication, @Valid @RequestBody UserAgreementRequest request) {
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
