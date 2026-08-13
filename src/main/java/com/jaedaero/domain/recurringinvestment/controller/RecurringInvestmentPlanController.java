package com.jaedaero.domain.recurringinvestment.controller;

import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanRequest;
import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanResponse;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanErrorCode;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanException;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentPlanService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/recurring-investment-plans")
@Api(tags = "적립식 투자 계획")
@RequiredArgsConstructor
public class RecurringInvestmentPlanController {

  private final RecurringInvestmentPlanService service;

  @GetMapping("/me")
  @ApiOperation(value = "내 적립식 투자 계획 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<RecurringInvestmentPlanResponse> get(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(service.get(authenticatedUserId(authentication)));
  }

  @PutMapping("/me")
  @ApiOperation(value = "내 적립식 투자 계획 설정 또는 변경")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<RecurringInvestmentPlanResponse> save(
      @ApiIgnore Authentication authentication,
      @Valid @RequestBody RecurringInvestmentPlanRequest request) {
    return ResponseEntity.ok(service.save(authenticatedUserId(authentication), request));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
