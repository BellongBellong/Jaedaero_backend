package com.jaedaero.domain.investmentguidance.controller;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceDetailResponse;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceErrorCode;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceException;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/investment-guidances")
@Api(tags = "적립식 투자 가이드")
@RequiredArgsConstructor
public class InvestmentGuidanceController {

  private final InvestmentGuidanceService service;

  @GetMapping("/latest")
  @ApiOperation(value = "최신 적립식 투자 가이드 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<InvestmentGuidanceResponse> getLatest(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(service.getLatest(authenticatedUserId(authentication)));
  }

  @GetMapping("/{guidanceId}")
  @ApiOperation(value = "적립식 투자 가이드 상세 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<InvestmentGuidanceDetailResponse> getDetail(
      @ApiIgnore Authentication authentication, @PathVariable long guidanceId) {
    return ResponseEntity.ok(service.getDetail(authenticatedUserId(authentication), guidanceId));
  }

  @PostMapping
  @ApiOperation(value = "적립식 투자 가이드 생성")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<InvestmentGuidanceResponse> create(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(authenticatedUserId(authentication)));
  }

  @PostMapping("/{guidanceId}/apply")
  @ApiOperation(value = "적립식 투자 가이드 적용")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<StrategyApplicationResponse> apply(
      @ApiIgnore Authentication authentication,
      @PathVariable long guidanceId,
      @Valid @RequestBody InvestmentGuidanceApplyRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.apply(authenticatedUserId(authentication), guidanceId, request));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
