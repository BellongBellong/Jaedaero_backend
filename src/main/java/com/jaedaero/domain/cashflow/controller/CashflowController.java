package com.jaedaero.domain.cashflow.controller;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/cashflow-forecasts")
@RequiredArgsConstructor
@Api(tags = "캐시플로우 예측")
public class CashflowController {

  private final CashflowService cashflowService;

  @PostMapping
  @ApiOperation(value = "캐시플로우 예측 생성", notes = "2026년 계급별 기본급과 최근 3개월 소비 평균으로 전역월까지 예측을 생성합니다.")
  @ApiResponses({
    @ApiResponse(code = 201, message = "생성 성공", response = CashflowForecastResponse.class),
    @ApiResponse(code = 409, message = "군 복무 정보 또는 목표 금액 미설정")
  })
  public ResponseEntity<CashflowForecastResponse> generate(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(cashflowService.generate(authenticatedUserId(authentication)));
  }

  @GetMapping("/latest")
  @ApiOperation(value = "최신 캐시플로우 예측 조회")
  public ResponseEntity<CashflowForecastResponse> getLatest(Authentication authentication) {
    return ResponseEntity.ok(cashflowService.getLatest(authenticatedUserId(authentication)));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new CashflowException(CashflowErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new CashflowException(CashflowErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
