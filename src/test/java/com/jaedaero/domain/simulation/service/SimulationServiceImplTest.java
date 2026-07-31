package com.jaedaero.domain.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.impl.SimulationServiceImpl;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceImplTest {

  @Test
  void previewIsCalculatedButNotPersisted_andSavedRequestCreatesHistory() {
    InMemorySimulationMapper mapper = new InMemorySimulationMapper();
    SimulationInputProvider provider =
        userId ->
            new SimulationInput(
                4_300_000L,
                10_950_000L,
                20_000_000L,
                LocalDate.of(2027, 9, 1),
                550_000L,
                new BigDecimal("5.00"),
                550_000L);
    SimulationService service = new SimulationServiceImpl(mapper, provider, new SimulationCalculator());

    SimulationResponse preview = service.run(1L, request(false));

    assertFalse(preview.getIsSaved());
    assertNull(preview.getSimulationId());
    assertEquals(0, mapper.countByUserId(1L));
    assertEquals(16_914_492L, preview.getExpectedAsset());

    SimulationResponse saved = service.run(1L, request(true));

    assertTrue(saved.getIsSaved());
    assertEquals(1L, saved.getSimulationId());
    SimulationHistoryResponse history = service.getHistory(1L, 0, 20);
    assertEquals(1, history.getTotalCount());
    assertEquals(saved.getSimulationId(), history.getSimulations().get(0).getSimulationId());
  }

  private SimulationRequest request(boolean isSaved) {
    SimulationRequest request = new SimulationRequest();
    request.setMonthlySavingAmount(300_000L);
    request.setMonthlySpendingAmount(180_000L);
    request.setInvestmentRatio(new BigDecimal("20.00"));
    request.setExpectedReturnRate(new BigDecimal("5.00"));
    request.setIsSaved(isSaved);
    return request;
  }

  private static class InMemorySimulationMapper implements SimulationMapper {

    private final List<SimulationVo> simulations = new ArrayList<>();

    @Override
    public int insert(SimulationVo simulation) {
      simulation.setSimulationId((long) simulations.size() + 1);
      simulations.add(simulation);
      return 1;
    }

    @Override
    public SimulationVo findByIdAndUserId(long simulationId, long userId) {
      return simulations.stream()
          .filter(item -> item.getSimulationId() == simulationId && item.getUserId() == userId)
          .findFirst()
          .orElse(null);
    }

    @Override
    public List<SimulationVo> findByUserId(long userId, int offset, int limit) {
      return simulations.stream()
          .filter(item -> item.getUserId() == userId)
          .sorted(Comparator.comparing(SimulationVo::getSimulationId).reversed())
          .skip(offset)
          .limit(limit)
          .toList();
    }

    @Override
    public long countByUserId(long userId) {
      return simulations.stream().filter(item -> item.getUserId() == userId).count();
    }
  }
}
