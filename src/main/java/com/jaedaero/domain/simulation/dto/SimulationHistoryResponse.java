package com.jaedaero.domain.simulation.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationHistoryResponse {

  private final List<SimulationResponse> simulations;
  private final int page;
  private final int size;
  private final long totalCount;
  private final boolean hasNext;
}
