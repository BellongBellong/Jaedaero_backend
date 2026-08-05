package com.jaedaero.domain.recurringinvestment.controller;

import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanRequest;
import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanResponse;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanErrorCode;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanException;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentPlanService;
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

@RestController
@RequestMapping("/api/v1/recurring-investment-plans")
@RequiredArgsConstructor
public class RecurringInvestmentPlanController {

  private final RecurringInvestmentPlanService service;

  @GetMapping("/me")
  public ResponseEntity<RecurringInvestmentPlanResponse> get(Authentication authentication) {
    return ResponseEntity.ok(service.get(authenticatedUserId(authentication)));
  }

  @PutMapping("/me")
  public ResponseEntity<RecurringInvestmentPlanResponse> save(
      Authentication authentication,
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
