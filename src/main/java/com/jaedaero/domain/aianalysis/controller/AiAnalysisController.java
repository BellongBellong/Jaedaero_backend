package com.jaedaero.domain.aianalysis.controller;

import com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.exception.AiAnalysisErrorCode;
import com.jaedaero.domain.aianalysis.exception.AiAnalysisException;
import com.jaedaero.domain.aianalysis.service.AiAnalysisService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.service.StrategyApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
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
@RequestMapping("/api/v1/ai-analyses")
@Api(tags = "AI 소비 분석")
@RequiredArgsConstructor
public class AiAnalysisController {
  private final AiAnalysisService service;
  private final StrategyApplicationService strategyApplicationService;
  @PostMapping
  @ApiImplicitParam(name = "X-User-Id", value = "개발 환경에서 사용할 목 데이터 사용자 ID", required = true, paramType = "header", example = "1")
  @ApiOperation(value = "AI 소비 분석 생성", notes = "현재월과 전월 동일 일수의 실제 거래 집계를 분석합니다. 저장하지 않은 What-if 기준이면 소비·군적금·투자·수익률 4개 값을 모두 요청에 전달합니다. 서버는 해당 기준의 예상 전역 자산, 추천 소비 한도와 절감 효과를 계산하며 gpt-4o-mini는 설명만 생성합니다.")
  public ResponseEntity<AiAnalysisResponse> analyze(@ApiIgnore Authentication auth, @RequestBody(required = false) AiAnalysisRequest request) {
    return ResponseEntity.status(HttpStatus.CREATED).body(service.analyze(userId(auth), request == null ? new AiAnalysisRequest() : request));
  }
  @GetMapping("/{analysisId}")
  @ApiImplicitParam(name = "X-User-Id", value = "개발 환경에서 사용할 목 데이터 사용자 ID", required = true, paramType = "header", example = "1")
  @ApiOperation(value = "AI 소비 분석 상세 조회")
  public ResponseEntity<AiAnalysisResponse> detail(@ApiIgnore Authentication auth, @PathVariable long analysisId) {
    return ResponseEntity.ok(service.getDetail(userId(auth), analysisId));
  }
  @PostMapping("/{analysisId}/apply")
  @ApiImplicitParam(name = "X-User-Id", value = "개발 환경에서 사용할 목 데이터 사용자 ID", required = true, paramType = "header", example = "1")
  @ApiOperation(value = "AI 소비 절감 전략 적용")
  public ResponseEntity<StrategyApplicationResponse> apply(
      @ApiIgnore Authentication auth, @PathVariable long analysisId) {
    return ResponseEntity.status(HttpStatus.CREATED)
        .body(strategyApplicationService.applyAiRecommendation(userId(auth), analysisId));
  }
  private long userId(Authentication auth) {
    if (auth == null || !auth.isAuthenticated() || auth instanceof AnonymousAuthenticationToken) throw new AiAnalysisException(AiAnalysisErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    try { return Long.parseLong(auth.getName()); } catch (NumberFormatException e) { throw new AiAnalysisException(AiAnalysisErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다."); }
  }
}
