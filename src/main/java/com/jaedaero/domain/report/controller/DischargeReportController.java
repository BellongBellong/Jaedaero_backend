package com.jaedaero.domain.report.controller;

import com.jaedaero.domain.report.dto.DischargeReportResponse;
import com.jaedaero.domain.report.service.DischargeReportService;
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
import org.springframework.web.server.ResponseStatusException;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@RequestMapping("/api/v1/reports")
@RequiredArgsConstructor
@Api(tags = "전역 리포트")
public class DischargeReportController {

  private final DischargeReportService dischargeReportService;

  @GetMapping("/discharge")
  @ApiOperation(
      value = "전역 예상 리포트 조회",
      notes = "최신 캐시플로우를 바탕으로 전역 예상 자산, 목표 달성률, 재정적 전역일을 반환합니다.")
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = DischargeReportResponse.class),
    @ApiResponse(code = 401, message = "인증 필요")
  })
  public ResponseEntity<DischargeReportResponse> getDischargeReport(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(dischargeReportService.get(authenticatedUserId(authentication)));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new ResponseStatusException(org.springframework.http.HttpStatus.UNAUTHORIZED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new ResponseStatusException(
          org.springframework.http.HttpStatus.UNAUTHORIZED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
