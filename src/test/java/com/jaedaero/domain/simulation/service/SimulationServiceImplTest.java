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
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.cashflow.service.CashflowCalculator;
import com.jaedaero.domain.cashflow.service.CashflowForecastCalculation;
import com.jaedaero.domain.cashflow.service.CashflowInput;
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
    assertEquals(15, preview.getCalculationDetail().getCalculationMonths());
    assertEquals(4_300_000L, preview.getCalculationDetail().getBaseAsset());
    assertEquals(18_300_000L, preview.getCalculationDetail().getExpectedSalary());
    assertEquals(2_700_000L, preview.getCalculationDetail().getExpectedSpending());
    assertEquals(15_600_000L, preview.getCalculationDetail().getCashflowIncreaseAmount());
    assertEquals(4_500_000L, preview.getCalculationDetail().getSoldierSavingPrincipal());
    assertEquals(2_250_000L, preview.getCalculationDetail().getInvestmentPrincipal());
    assertEquals(8_850_000L, preview.getCalculationDetail().getUnallocatedPrincipal());
    assertEquals(131_250L, preview.getExpectedEffect().getSoldierSavingInterest());
    assertEquals(4_500_000L, preview.getExpectedEffect().getGovernmentMatchingSupport());
    assertEquals(66_825L, preview.getExpectedEffect().getExpectedInvestmentReturn());
    assertEquals(4_698_075L, preview.getExpectedEffect().getProjectedBenefitAmount());
    assertFalse(preview.getExpectedEffect().getReturnsIncludedInExpectedAsset());
    assertEquals(19_900_000L, preview.getExpectedEffect().getConservativeExpectedAsset());
    assertEquals(24_598_075L, preview.getExpectedEffect().getPotentialExpectedAsset());
    assertEquals(
        Math.addExact(
            preview.getExpectedAsset(), preview.getExpectedEffect().getProjectedBenefitAmount()),
        preview.getExpectedEffect().getPotentialExpectedAsset());
    assertEquals(
        SimulationCalculator.CALCULATION_POLICY_VERSION,
        preview.getExpectedEffect().getCalculationPolicyVersion());

    SimulationResponse saved = service.run(1L, request(true));

    assertTrue(saved.getIsSaved());
    assertEquals(1L, saved.getSimulationId());
    assertEquals(20_000_000L, saved.getTargetAmount());
    SimulationHistoryResponse history = service.getHistory(1L, 0, 20);
    assertEquals(1, history.getTotalCount());
    assertEquals(saved.getSimulationId(), history.getSimulations().get(0).getSimulationId());
    assertEquals(150_000L, history.getSimulations().get(0).getMonthlyInvestmentAmount());
    assertEquals(new BigDecimal("16.67"), history.getSimulations().get(0).getInvestmentRate());
    assertEquals(
        saved.getExpectedEffect().getPotentialExpectedAsset(),
        history.getSimulations().get(0).getExpectedEffect().getPotentialExpectedAsset());
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

  @Test
  void defaultsUseLatestAppliedAiStrategy() {
    SimulationInputProvider provider =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                400_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1),
                new AppliedCashflowStrategy(
                    9L, 120_000L, 550_000L, 80_000L, new BigDecimal("6.50")));
    SimulationService service =
        new SimulationServiceImpl(
            new InMemorySimulationMapper(),
            provider,
            simulationCalculator(),
            new SimulationAllocationPolicy(),
            FIXED_CLOCK);

    SimulationDefaultsResponse defaults = service.getDefaults(1L);

    assertEquals(120_000L, defaults.getMonthlySpendingAmount());
    assertEquals(550_000L, defaults.getMonthlySavingAmount());
    assertEquals(80_000L, defaults.getMonthlyInvestmentAmount());
    assertEquals(new BigDecimal("6.50"), defaults.getExpectedReturnRate());
    assertEquals(new BigDecimal("13.33"), defaults.getSpendingRate());
    assertEquals(new BigDecimal("61.11"), defaults.getSavingRate());
    assertEquals(new BigDecimal("8.89"), defaults.getInvestmentRate());
    assertEquals(150_000L, defaults.getUnallocatedAmount());
  }

  @Test
  void lowerSoldierSavingReducesInterestAndMatching_withoutDoubleCountingPrincipal() {
    SimulationService service = service(900_000L);
    SimulationRequest highSaving = request(false);
    highSaving.setMonthlySavingAmount(300_000L);
    highSaving.setMonthlyInvestmentAmount(150_000L);
    SimulationRequest lowSaving = request(false);
    lowSaving.setMonthlySavingAmount(100_000L);
    lowSaving.setMonthlyInvestmentAmount(350_000L);

    SimulationResponse high = service.run(1L, highSaving);
    SimulationResponse low = service.run(1L, lowSaving);

    assertEquals(high.getExpectedAsset(), low.getExpectedAsset());
    assertTrue(
        high.getExpectedEffect().getPotentialExpectedAsset()
            > low.getExpectedEffect().getPotentialExpectedAsset());
    assertTrue(
        high.getExpectedEffect().getSoldierSavingInterest()
            > low.getExpectedEffect().getSoldierSavingInterest());
    assertTrue(
        high.getExpectedEffect().getGovernmentMatchingSupport()
            > low.getExpectedEffect().getGovernmentMatchingSupport());
    assertEquals(
        high.getCalculationDetail().getCashflowIncreaseAmount(),
        Math.addExact(
            Math.addExact(
                high.getCalculationDetail().getSoldierSavingPrincipal(),
                high.getCalculationDetail().getInvestmentPrincipal()),
            high.getCalculationDetail().getUnallocatedPrincipal()));
  }

  @Test
  void expectedReturnRateOnlyChangesPotentialAsset_notConservativeForecast() {
    SimulationService service = service(900_000L);
    SimulationRequest zeroReturn = request(false);
    zeroReturn.setExpectedReturnRate(BigDecimal.ZERO);
    SimulationRequest highReturn = request(false);
    highReturn.setExpectedReturnRate(new BigDecimal("10.00"));

    SimulationResponse conservative = service.run(1L, zeroReturn);
    SimulationResponse optimistic = service.run(1L, highReturn);

    assertEquals(conservative.getExpectedAsset(), optimistic.getExpectedAsset());
    assertEquals(
        conservative.getFinancialDischargeDate(), optimistic.getFinancialDischargeDate());
    assertTrue(
        optimistic.getExpectedEffect().getPotentialExpectedAsset()
            > conservative.getExpectedEffect().getPotentialExpectedAsset());
  }

  @Test
  void identicalMonthlyInputsProduceTheSameCashflowAndWhatIfForecast() {
    MilitaryPayPolicyMapper payMapper =
        (soldierType, rankName, monthStart, monthEnd) -> 900_000L;
    DefaultMilitaryPayPolicy payPolicy = new DefaultMilitaryPayPolicy(payMapper);
    LocalDate calculationDate = LocalDate.of(2026, 7, 31);
    LocalDate dischargeDate = LocalDate.of(2027, 9, 1);
    AppliedCashflowStrategy strategy =
        new AppliedCashflowStrategy(
            9L, 180_000L, 300_000L, 150_000L, new BigDecimal("5.00"));
    CashflowForecastCalculation cashflow =
        new CashflowCalculator(payPolicy)
            .calculate(
                new CashflowInput(
                    4_300_000L,
                    20_000_000L,
                    180_000L,
                    SoldierType.ARMY,
                    LocalDate.of(2026, 3, 1),
                    dischargeDate,
                    List.of(),
                    strategy),
                calculationDate);
    SimulationRequest whatIfRequest = request(false);
    SimulationCalculationResult whatIf =
        new SimulationCalculator(payPolicy)
            .calculate(
                new SimulationInput(
                    4_300_000L,
                    20_000_000L,
                    180_000L,
                    SoldierType.ARMY,
                    LocalDate.of(2026, 3, 1),
                    dischargeDate),
                whatIfRequest,
                calculationDate);

    assertEquals(cashflow.expectedAsset(), whatIf.expectedAsset());
    assertEquals(cashflow.financialDischargeDate(), whatIf.financialDischargeDate());
    assertEquals(cashflow.soldierSavingInterest(), whatIf.soldierSavingInterest());
    assertEquals(cashflow.governmentMatchingSupport(), whatIf.governmentMatchingSupport());
    assertEquals(cashflow.expectedInvestmentReturn(), whatIf.expectedInvestmentReturn());
    assertEquals(cashflow.potentialExpectedAsset(), whatIf.potentialExpectedAsset());
  }

  @Test
  void legacySimulationWithoutDetailSnapshotReturnsNullableDetail() {
    SimulationVo legacy =
        SimulationVo.builder()
            .simulationId(1L)
            .scenarioName("기존 이력")
            .targetAmount(20_000_000L)
            .monthlySpendingAmount(180_000L)
            .monthlySavingAmount(300_000L)
            .monthlyInvestmentAmount(150_000L)
            .expectedReturnRate(new BigDecimal("5.00"))
            .expectedAsset(19_900_000L)
            .isSaved(true)
            .build();

    SimulationResponse response =
        SimulationResponse.from(
            legacy,
            new SimulationAllocationMetrics(
                900_000L,
                new BigDecimal("20.00"),
                new BigDecimal("33.33"),
                new BigDecimal("16.67"),
                270_000L));

    assertNull(response.getCalculationDetail());
    assertNull(response.getExpectedEffect());
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
