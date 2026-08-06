package com.jaedaero.domain.simulation.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.simulation.dto.SimulationDefaultsResponse;
import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.impl.SimulationServiceImpl;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class SimulationServiceImplTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(
          Instant.parse("2026-07-31T00:00:00Z"),
          ZoneId.of("Asia/Seoul"));

  @Test
  void previewIsCalculatedButNotPersisted_andSavedRequestCreatesHistory() {
    InMemorySimulationMapper mapper = new InMemorySimulationMapper();
    SimulationInputProvider provider =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                400_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    SimulationService service =
        new SimulationServiceImpl(
            mapper,
            provider,
            simulationCalculator(),
            new SimulationAllocationPolicy(),
            FIXED_CLOCK);

    SimulationDefaultsResponse defaults = service.getDefaults(1L);
    assertEquals(20_000_000L, defaults.getTargetAmount());
    assertEquals(900_000L, defaults.getReferenceMonthlyIncome());
    assertEquals(350_000L, defaults.getMonthlySpendingAmount());
    assertEquals(550_000L, defaults.getMonthlySavingAmount());
    assertEquals(0L, defaults.getMonthlyInvestmentAmount());
    assertEquals(new BigDecimal("5.00"), defaults.getExpectedReturnRate());
    assertEquals(new BigDecimal("38.89"), defaults.getSpendingRate());
    assertEquals(new BigDecimal("61.11"), defaults.getSavingRate());
    assertEquals(0L, defaults.getUnallocatedAmount());

    SimulationResponse preview = service.run(1L, request(false));

    assertFalse(preview.getIsSaved());
    assertNull(preview.getSimulationId());
    assertEquals(0, mapper.countByUserId(1L));
    assertEquals(19_900_000L, preview.getExpectedAsset());
    assertEquals(20_000_000L, preview.getTargetAmount());
    assertEquals(150_000L, preview.getMonthlyInvestmentAmount());
    assertEquals(900_000L, preview.getReferenceMonthlyIncome());
    assertEquals(new BigDecimal("20.00"), preview.getSpendingRate());
    assertEquals(new BigDecimal("33.33"), preview.getSavingRate());
    assertEquals(new BigDecimal("16.67"), preview.getInvestmentRate());
    assertEquals(270_000L, preview.getUnallocatedAmount());

    SimulationResponse saved = service.run(1L, request(true));

    assertTrue(saved.getIsSaved());
    assertEquals(1L, saved.getSimulationId());
    assertEquals(20_000_000L, saved.getTargetAmount());
    SimulationHistoryResponse history = service.getHistory(1L, 0, 20);
    assertEquals(1, history.getTotalCount());
    assertEquals(saved.getSimulationId(), history.getSimulations().get(0).getSimulationId());
    assertEquals(150_000L, history.getSimulations().get(0).getMonthlyInvestmentAmount());
    assertEquals(new BigDecimal("16.67"), history.getSimulations().get(0).getInvestmentRate());
  }

  @Test
  void allocationOverReferenceIncome_isRejected() {
    SimulationService service = service(900_000L);
    SimulationRequest request = request(false);
    request.setMonthlySpendingAmount(300_000L);
    request.setMonthlySavingAmount(550_000L);
    request.setMonthlyInvestmentAmount(50_001L);

    SimulationException exception =
        assertThrows(SimulationException.class, () -> service.run(1L, request));

    assertEquals(SimulationErrorCode.INVALID_REQUEST, exception.getErrorCode());
  }

  @Test
  void savingOverPolicyLimit_isRejected() {
    SimulationService service = service(1_500_000L);
    SimulationRequest request = request(false);
    request.setMonthlySavingAmount(550_001L);

    SimulationException exception =
        assertThrows(SimulationException.class, () -> service.run(1L, request));

    assertEquals(SimulationErrorCode.INVALID_REQUEST, exception.getErrorCode());
  }

  private SimulationRequest request(boolean isSaved) {
    SimulationRequest request = new SimulationRequest();
    request.setMonthlySavingAmount(300_000L);
    request.setMonthlySpendingAmount(180_000L);
    request.setMonthlyInvestmentAmount(150_000L);
    request.setExpectedReturnRate(new BigDecimal("5.00"));
    request.setIsSaved(isSaved);
    return request;
  }

  private SimulationService service(long monthlySalary) {
    SimulationInputProvider provider =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                180_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    MilitaryPayPolicyMapper payMapper =
        (soldierType, rankName, monthStart, monthEnd) -> monthlySalary;
    return new SimulationServiceImpl(
        new InMemorySimulationMapper(),
        provider,
        new SimulationCalculator(new DefaultMilitaryPayPolicy(payMapper)),
        new SimulationAllocationPolicy(),
        FIXED_CLOCK);
  }

  private SimulationCalculator simulationCalculator() {
    MilitaryPayPolicyMapper payMapper =
        (soldierType, rankName, monthStart, monthEnd) ->
            switch (rankName) {
              case "이병" -> 750_000L;
              case "일병" -> 900_000L;
              case "상병" -> 1_200_000L;
              case "병장" -> 1_500_000L;
              default -> null;
            };
    return new SimulationCalculator(new DefaultMilitaryPayPolicy(payMapper));
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
