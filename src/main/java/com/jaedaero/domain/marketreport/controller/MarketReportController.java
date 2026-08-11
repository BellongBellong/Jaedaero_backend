package com.jaedaero.domain.marketreport.controller;

import com.jaedaero.domain.marketreport.dto.TodayMarketReportResponse;
import com.jaedaero.domain.marketreport.exception.MarketReportErrorCode;
import com.jaedaero.domain.marketreport.exception.MarketReportException;
import com.jaedaero.domain.marketreport.service.MarketReportGenerationService;
import com.jaedaero.domain.marketreport.service.MarketReportService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/market-reports")
@Api(tags = "오늘의 AI 시장 리포트")
public class MarketReportController {

  private final MarketReportService marketReportService;
  private final MarketReportGenerationService marketReportGenerationService;
  private final String appEnvironment;

  public MarketReportController(
      MarketReportService marketReportService,
      MarketReportGenerationService marketReportGenerationService,
      @Value("${app.environment:production}") String appEnvironment) {
    this.marketReportService = marketReportService;
    this.marketReportGenerationService = marketReportGenerationService;
    this.appEnvironment = appEnvironment;
  }

  @GetMapping("/today")
  @ApiOperation(value = "오늘의 AI 시장 리포트 조회")
  public ResponseEntity<TodayMarketReportResponse> getToday() {
    return ResponseEntity.ok(marketReportService.getToday());
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
}
