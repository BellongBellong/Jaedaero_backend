package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;

public interface SimulationService {

  SimulationResponse run(long userId, SimulationRequest request);

  SimulationHistoryResponse getHistory(long userId, int page, int size);

  SimulationResponse getDetail(long userId, long simulationId);
}
