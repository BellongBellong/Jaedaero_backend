package com.jaedaero.domain.simulation.exception;

import com.jaedaero.global.common.exception.BusinessException;

public class SimulationException extends BusinessException {

  public SimulationException(SimulationErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
