package com.jaedaero.domain.simulation.controller;

import com.jaedaero.domain.simulation.dto.SimulationDefaultsResponse;
import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.service.SimulationService;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiImplicitParam;
import io.swagger.annotations.ApiOperation;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import springfox.documentation.annotations.ApiIgnore;

@RestController
@Validated
@RequestMapping("/api/v1/simulations")
@Api(tags = "What-if 시뮬레이션")
@RequiredArgsConstructor
public class SimulationController {

  private final SimulationService simulationService;

  @GetMapping("/defaults")
  @ApiOperation(value = "What-if 최초 입력값 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<SimulationDefaultsResponse> getDefaults(
      @ApiIgnore Authentication authentication) {
    return ResponseEntity.ok(
        simulationService.getDefaults(authenticatedUserId(authentication)));
  }

  @PostMapping
  @ApiOperation(value = "What-if 시뮬레이션 실행")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<SimulationResponse> run(
      @ApiIgnore Authentication authentication, @Valid @RequestBody SimulationRequest request) {
    SimulationResponse response = simulationService.run(authenticatedUserId(authentication), request);
    return response.getIsSaved()
        ? ResponseEntity.status(HttpStatus.CREATED).body(response)
        : ResponseEntity.ok(response);
  }

  @GetMapping
  @ApiOperation(value = "저장된 What-if 시뮬레이션 이력 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<SimulationHistoryResponse> getHistory(
      @ApiIgnore Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(simulationService.getHistory(authenticatedUserId(authentication), page, size));
  }

  @GetMapping("/{simulationId}")
  @ApiOperation(value = "What-if 시뮬레이션 상세 조회")
  @ApiImplicitParam(
      name = "X-User-Id",
      value = "개발 환경에서 사용할 목 데이터 사용자 ID",
      required = true,
      paramType = "header",
      example = "1")
  public ResponseEntity<SimulationResponse> getDetail(
      @ApiIgnore Authentication authentication, @PathVariable long simulationId) {
    return ResponseEntity.ok(
        simulationService.getDetail(authenticatedUserId(authentication), simulationId));
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null
        || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new SimulationException(SimulationErrorCode.UNAUTHENTICATED, "로그인이 필요합니다.");
    }
    try {
      return Long.parseLong(authentication.getName());
    } catch (NumberFormatException exception) {
      throw new SimulationException(SimulationErrorCode.UNAUTHENTICATED, "유효한 사용자 인증 정보가 없습니다.");
    }
  }
}
