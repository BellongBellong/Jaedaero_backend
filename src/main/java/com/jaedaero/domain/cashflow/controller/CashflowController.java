package com.jaedaero.domain.cashflow.controller;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/cashflow")
@RequiredArgsConstructor
@Api(tags = "캐시플로우")
public class CashflowController {

  private final CashflowService cashflowService;

  @GetMapping
  @ApiOperation(value = "월별 자산 흐름 조회", notes = "최신 캐시플로우 예측에서 요청한 개월 수만 반환합니다.")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<CashflowForecastResponse> getCashflow(
      @RequestParam int months, @ApiIgnore Authentication authentication) {
    if (months < 1) {
      throw new CashflowException(CashflowErrorCode.INVALID_MONTHS, "months는 1 이상이어야 합니다.");
    }
    return ResponseEntity.ok(cashflowService.getLatest(authenticatedUserId(authentication), months));
  }

  @PostMapping
  @ApiOperation(value = "캐시플로우 예측 생성", notes = "현재 자산·소비·봉급 정책을 기준으로 새 예측을 생성합니다.")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<CashflowForecastResponse> generate(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(cashflowService.generate(authenticatedUserId(authentication)));
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
