package com.jaedaero.domain.simulation.exception;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum SimulationErrorCode implements ErrorCode {
  UNAUTHENTICATED(HttpStatus.UNAUTHORIZED, "SIMULATION_UNAUTHENTICATED"),
  NOT_FOUND(HttpStatus.NOT_FOUND, "SIMULATION_NOT_FOUND"),
  INPUT_NOT_READY(HttpStatus.CONFLICT, "SIMULATION_INPUT_NOT_READY");

  private final HttpStatus status;
  private final String code;

  SimulationErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
