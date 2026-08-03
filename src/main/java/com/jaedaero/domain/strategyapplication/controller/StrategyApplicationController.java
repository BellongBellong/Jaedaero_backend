package com.jaedaero.domain.strategyapplication.controller;

import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.exception.StrategyApplicationErrorCode;
import com.jaedaero.domain.strategyapplication.exception.StrategyApplicationException;
import com.jaedaero.domain.strategyapplication.service.StrategyApplicationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import java.util.List;
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
@RequestMapping("/api/v1/strategy-applications")
@Api(tags = "전략 적용 이력")
@RequiredArgsConstructor
public class StrategyApplicationController {

  private final StrategyApplicationService strategyApplicationService;

  @GetMapping
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  @ApiOperation(value = "전략 적용 이력 조회")
  public ResponseEntity<List<StrategyApplicationResponse>> getHistory(
      @ApiIgnore Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(
        strategyApplicationService.getHistory(authenticatedUserId(authentication), page, size));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new StrategyApplicationException(
          StrategyApplicationErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
