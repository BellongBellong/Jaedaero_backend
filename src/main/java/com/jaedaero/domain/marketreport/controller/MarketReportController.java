package com.jaedaero.domain.marketreport.controller;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.dto.TodayMarketIndicatorsResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportGenerationService;
import com.jaedaero.domain.marketreport.service.MarketReportIndicatorRefreshService;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiImplicitParam;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestHeader;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;

@RestController
@RequestMapping("/api/v1/market-reports")
@Api(tags = "오늘의 AI 시장 리포트")
public class MarketReportController {

  private final MarketReportService marketReportService;
  private final MarketReportGenerationService marketReportGenerationService;
  private final MarketReportIndicatorRefreshService indicatorRefreshService;
  private final String appEnvironment;
  private final String adminRefreshToken;

  public MarketReportController(
      MarketReportService marketReportService,
      MarketReportGenerationService marketReportGenerationService,
      MarketReportIndicatorRefreshService indicatorRefreshService,
      @Value("${app.environment:production}") String appEnvironment,
      @Value("${market-report.admin-refresh-token:}") String adminRefreshToken) {
    this.marketReportService = marketReportService;
    this.marketReportGenerationService = marketReportGenerationService;
    this.indicatorRefreshService = indicatorRefreshService;
    this.appEnvironment = appEnvironment;
    this.adminRefreshToken = adminRefreshToken;
  }

  @GetMapping("/today")
  @ApiOperation(value = "오늘의 AI 시장 리포트 조회")
  public ResponseEntity<TodayMarketReportResponse> getToday() {
    return ResponseEntity.ok(marketReportService.getToday());
  }

  @GetMapping("/today/indicators")
  @ApiOperation(value = "오늘의 시장 지표 조회")
  public ResponseEntity<TodayMarketIndicatorsResponse> getTodayIndicators() {
    return ResponseEntity.ok(marketReportService.getTodayIndicators());
  }

  @PostMapping("/today/indicators/refresh")
  @ApiOperation(value = "[운영 관리자] 시장 지표 재수집", notes = "PARTIAL 상태의 현재 노출 리포트가 없으면 최신 리포트의 지표 4종을 재수집합니다. Gemini 본문과 출처는 재생성하지 않습니다.")
  @ApiImplicitParam(
      name = "X-Market-Report-Admin-Token",
      value = "Railway MARKET_REPORT_ADMIN_REFRESH_TOKEN과 일치하는 운영 재수집 토큰",
      required = true,
      paramType = "header")
  public ResponseEntity<TodayMarketIndicatorsResponse> refreshTodayIndicators(
      @RequestHeader(value = "X-Market-Report-Admin-Token", required = false) String token) {
    requireAdminRefreshToken(token);
    indicatorRefreshService.refreshToday();
    return ResponseEntity.accepted().body(marketReportService.getTodayIndicators());
  }

  @PostMapping("/generate")
  @ApiOperation(
      value = "[로컬 전용] 오늘의 AI 시장 리포트 수동 생성",
      notes =
          "app.environment=local일 때만 동작한다. 매일 17:00 KST 배치를 기다리지 않고 즉시"
              + " FALLBACK 또는 pending 당일 행만 Gemini 재시험한다. 성공 GEMINI 리포트와 동시"
              + " in-progress 실행은 재호출하지 않으며, 응답 본문으로 현재 상태와 출처를 확인할 수 있다.")
  public ResponseEntity<TodayMarketReportResponse> generateNow() {
    requireLocalEnvironment();
    marketReportGenerationService.generateForTodayForLocalRetry();
    return ResponseEntity.accepted().body(marketReportService.getToday());
  }

  private void requireLocalEnvironment() {
    if (!"local".equalsIgnoreCase(appEnvironment)) {
      throw new MarketReportException(
          MarketReportErrorCode.NOT_FOUND, "이 엔드포인트는 로컬 환경에서만 사용할 수 있습니다.");
    }
  }

  private void requireAdminRefreshToken(String token) {
    if (adminRefreshToken == null
        || adminRefreshToken.isBlank()
        || token == null
        || !MessageDigest.isEqual(
            adminRefreshToken.getBytes(StandardCharsets.UTF_8), token.getBytes(StandardCharsets.UTF_8))) {
      throw new MarketReportException(
          MarketReportErrorCode.ADMIN_REFRESH_FORBIDDEN, "운영 시장 지표 재수집 권한이 없습니다.");
    }
  }
}
