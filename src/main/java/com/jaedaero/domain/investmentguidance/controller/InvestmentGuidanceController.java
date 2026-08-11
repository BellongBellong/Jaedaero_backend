package com.jaedaero.domain.investmentguidance.controller;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceDetailResponse;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceErrorCode;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceException;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
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

@RestController
@RequestMapping("/api/v1/investment-guidances")
@RequiredArgsConstructor
public class InvestmentGuidanceController {

  private final InvestmentGuidanceService service;

  @GetMapping("/latest")
  public ResponseEntity<InvestmentGuidanceResponse> getLatest(Authentication authentication) {
    return ResponseEntity.ok(service.getLatest(authenticatedUserId(authentication)));
  }

  @GetMapping("/{guidanceId}")
  public ResponseEntity<InvestmentGuidanceDetailResponse> getDetail(
      Authentication authentication, @PathVariable long guidanceId) {
    return ResponseEntity.ok(service.getDetail(authenticatedUserId(authentication), guidanceId));
  }

  @PostMapping
  public ResponseEntity<InvestmentGuidanceResponse> create(Authentication authentication) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(service.create(authenticatedUserId(authentication)));
  }

  @PostMapping("/{guidanceId}/apply")
  public ResponseEntity<StrategyApplicationResponse> apply(
      Authentication authentication,
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
