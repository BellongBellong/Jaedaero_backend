package com.jaedaero.domain.simulation.controller;

import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.service.SimulationService;
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

@RestController
@Validated
@RequestMapping("/api/v1/simulations")
@RequiredArgsConstructor
public class SimulationController {

  private final SimulationService simulationService;

  @PostMapping
  public ResponseEntity<SimulationResponse> run(
      Authentication authentication, @Valid @RequestBody SimulationRequest request) {
    SimulationResponse response = simulationService.run(authenticatedUserId(authentication), request);
    return response.getIsSaved()
        ? ResponseEntity.status(HttpStatus.CREATED).body(response)
        : ResponseEntity.ok(response);
  }

  @GetMapping
  public ResponseEntity<SimulationHistoryResponse> getHistory(
      Authentication authentication,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(100) int size) {
    return ResponseEntity.ok(simulationService.getHistory(authenticatedUserId(authentication), page, size));
  }

  @GetMapping("/{simulationId}")
  public ResponseEntity<SimulationResponse> getDetail(
      Authentication authentication, @PathVariable long simulationId) {
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
