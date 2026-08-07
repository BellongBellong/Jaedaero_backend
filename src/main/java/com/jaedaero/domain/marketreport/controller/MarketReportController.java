package com.jaedaero.domain.marketreport.controller;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/market-reports")
@Api(tags = "오늘의 AI 투자 리포트")
@RequiredArgsConstructor
public class MarketReportController {

  private final MarketReportService marketReportService;

  @GetMapping("/today")
  @ApiOperation(value = "오늘의 시장 리포트 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<TodayMarketReportResponse> getToday(
      @ApiIgnore Authentication authentication) {
    requireAuthenticated(authentication);
    return ResponseEntity.ok(marketReportService.getToday());
  }

  private void requireAuthenticated(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new MarketReportException(
          MarketReportErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
  }
}
