package com.jaedaero.domain.dashboard.controller;

import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.service.DashboardService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
@Api(tags = "대시보드")
public class DashboardController {

  private final DashboardService dashboardService;

  @GetMapping
  @ApiOperation(
      value = "홈 대시보드 요약 조회",
      notes = "가장 최근 생성된 캐시플로우 예측을 기준으로 자산·전역일·이번 달 소비/저축 요약을 반환합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = DashboardResponse.class),
    @ApiResponse(code = 401, message = "인증 필요 (CASHFLOW_UNAUTHENTICATED)"),
    @ApiResponse(code = 404, message = "생성된 캐시플로우 예측이 없음 (CASHFLOW_NOT_FOUND)")
  })
  public ResponseEntity<DashboardResponse> get(@ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(dashboardService.get(authenticatedUserId(authentication)));
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
