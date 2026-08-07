package com.jaedaero.domain.analysishistory.controller;

import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryFilter;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryPageResponse;
import com.jaedaero.domain.analysishistory.exception.AnalysisHistoryErrorCode;
import com.jaedaero.domain.analysishistory.exception.AnalysisHistoryException;
import com.jaedaero.domain.analysishistory.service.AnalysisHistoryService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Validated
@RequestMapping("/api/v1/analysis-histories")
@Api(tags = "AI·What-if 통합 분석 이력")
@RequiredArgsConstructor
public class AnalysisHistoryController {

  private final AnalysisHistoryService analysisHistoryService;

  @GetMapping
  @ApiOperation(value = "AI·What-if 통합 분석 이력 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<AnalysisHistoryPageResponse> getHistories(
      @ApiIgnore Authentication authentication,
      @RequestParam(defaultValue = "ALL") AnalysisHistoryFilter type,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(
        analysisHistoryService.getHistories(
            authenticatedUserId(authentication), type, page, size));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new AnalysisHistoryException(
          AnalysisHistoryErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new AnalysisHistoryException(
          AnalysisHistoryErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
