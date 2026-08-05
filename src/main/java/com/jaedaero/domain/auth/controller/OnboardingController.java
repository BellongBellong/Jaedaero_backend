package com.jaedaero.domain.auth.controller;

import com.jaedaero.domain.auth.dto.InvestmentPreferenceRequest;
import com.jaedaero.domain.auth.dto.InvestmentPreferenceResponse;
import com.jaedaero.domain.auth.dto.MilitaryInfoRequest;
import com.jaedaero.domain.auth.dto.SoldierProfileResponse;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.MilitaryInfoException;
import com.jaedaero.domain.auth.service.InvestmentPreferenceService;
import com.jaedaero.domain.auth.service.MilitaryInfoService;
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
@RequestMapping("/api/v1/onboarding")
@RequiredArgsConstructor
@Api(tags = "회원가입")
public class OnboardingController {

  private final MilitaryInfoService militaryInfoService;
  private final InvestmentPreferenceService investmentPreferenceService;

  @PostMapping("/military-info")
  @ApiOperation(value = "군인 정보 등록", notes = "전역 예정일과 동기 챌린지 그룹을 자동으로 계산합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "등록 성공", response = SoldierProfileResponse.class),
    @ApiResponse(code = 400, message = "군종, 계급 또는 입대일이 올바르지 않음"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<SoldierProfileResponse> registerMilitaryInfo(
      @ApiIgnore Authentication authentication, @Valid @RequestBody MilitaryInfoRequest request) {
    return ResponseEntity.ok(
        militaryInfoService.registerMilitaryInfo(getAuthenticatedUserId(authentication), request));
  }

  @PostMapping("/investment-preference")
  @ApiOperation(
      value = "투자성향 시드 설정 / 목표 금액 설정",
      notes = "투자성향 콜드스타트 시드와 목표 전역 자금을 한 요청으로 저장합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "저장 성공", response = InvestmentPreferenceResponse.class),
    @ApiResponse(code = 400, message = "투자 성향 또는 목표 금액이 올바르지 않음"),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<InvestmentPreferenceResponse> registerInvestmentPreference(
      @ApiIgnore Authentication authentication,
      @Valid @RequestBody InvestmentPreferenceRequest request) {
    return ResponseEntity.ok(
        investmentPreferenceService.registerInvestmentPreference(
            getAuthenticatedUserId(authentication), request));
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
