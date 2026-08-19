package com.jaedaero.domain.dashboard.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngine;
import com.jaedaero.domain.cashflow.service.SoldierSavingInput;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardSpendingMapper;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardServiceImplTest {

  @Test
  void generatesInitialForecastWhenNoForecastExists() {
    CashflowForecastResponse generated =
        CashflowForecastResponse.builder()
            .baseAsset(1_000_000L)
            .expectedAsset(10_000_000L)
            .achievementRate(new BigDecimal("50.00"))
            .months(List.of())
            .build();
    RecordingCashflowService cashflowService = new RecordingCashflowService(generated);
    DashboardMapper dashboardMapper = userId -> LocalDate.of(2027, 6, 20);
    DashboardSpendingMapper dashboardSpendingMapper = userId -> 0L;
    StrategyApplicationMapper strategyApplicationMapper = new EmptyStrategyApplicationMapper();
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            dashboardMapper,
            strategyApplicationMapper,
            dashboardSpendingMapper,
            Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    DashboardResponse response = service.get(1L);

    assertEquals(1, cashflowService.generateCount);
    assertEquals(1_500_000L, response.getCurrentAsset());
    assertEquals(10_000_000L, response.getExpectedAsset());
  }

  @Test
  void usesLatestSavedWhatIfAsDashboardFinancialGoal() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder()
            .baseAsset(1_000_000L)
            .expectedAsset(10_000_000L)
            .achievementRate(new BigDecimal("50.00"))
            .months(List.of())
            .build();
    SimulationVo simulation =
        SimulationVo.builder()
            .simulationId(7L)
            .targetAmount(20_000_000L)
            .monthlySpendingAmount(120_000L)
            .monthlyInvestmentAmount(180_000L)
            .expectedAsset(21_000_000L)
            .financialDischargeDate(LocalDate.of(2027, 3, 15))
            .build();
    RecordingCashflowService cashflowService = new RecordingCashflowService(cashflow);
    cashflowService.calculationInput =
        CashflowCalculationInputResponse.builder()
            .baseAsset(1_500_000L)
            .targetAmount(20_000_000L)
            .soldierSavings(List.of())
            .build();
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            userId -> LocalDate.of(2027, 6, 20),
            new EmptyStrategyApplicationMapper(),
            userId -> 0L,
            new LatestSimulationMapper(simulation),
            FIXED_CLOCK);

    DashboardResponse response = service.get(1L);

    assertEquals(21_000_000L, response.getExpectedAsset());
    assertEquals(LocalDate.of(2027, 3, 15), response.getFinancialDischargeDate());
    assertEquals(120_000L, response.getMonthlySpendingGoal());
    assertEquals(180_000L, response.getMonthlyInvestmentGoal());
    assertEquals("SIMULATION", response.getGoalSource());
    assertEquals(20_000_000L, response.getTargetAmount());
  }

  @Test
  void ignoresSavedWhatIfWhenItsTargetDiffersFromCurrentGoal() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder()
            .calculationPolicyVersion(ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION)
            .expectedAsset(12_000_000L)
            .achievementRate(new BigDecimal("60.00"))
            .months(List.of())
            .build();
    RecordingCashflowService cashflowService = new RecordingCashflowService(cashflow);
    cashflowService.calculationInput =
        CashflowCalculationInputResponse.builder()
            .baseAsset(1_500_000L)
            .targetAmount(25_000_000L)
            .soldierSavings(List.of())
            .build();
    SimulationVo oldSimulation =
        SimulationVo.builder()
            .targetAmount(20_000_000L)
            .expectedAsset(21_000_000L)
            .monthlySpendingAmount(120_000L)
            .monthlyInvestmentAmount(180_000L)
            .build();
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            userId -> LocalDate.of(2027, 6, 20),
            new EmptyStrategyApplicationMapper(),
            userId -> 0L,
            new LatestSimulationMapper(oldSimulation),
            FIXED_CLOCK);

    DashboardResponse response = service.get(1L);

    assertEquals(12_000_000L, response.getExpectedAsset());
    assertEquals(25_000_000L, response.getTargetAmount());
    assertEquals(null, response.getGoalSource());
  }

  @Test
  void includesCurrentSavingBenefitsInCurrentExpectedAsset() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder().calculationPolicyVersion(
            ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION).months(List.of()).build();
    RecordingCashflowService cashflowService = new RecordingCashflowService(cashflow);
    cashflowService.calculationInput =
        CashflowCalculationInputResponse.builder()
            .baseAsset(1_917_500L)
            .soldierSavings(
                List.of(
                    new SoldierSavingInput(
                        1_100_000L,
                        550_000L,
                        new BigDecimal("5.00"),
                        0L,
                        LocalDate.of(2026, 1, 1),
                        LocalDate.of(2027, 12, 15))))
            .build();
    DashboardServiceImpl service =
        new DashboardServiceImpl(
            cashflowService,
            userId -> LocalDate.of(2027, 12, 15),
            new EmptyStrategyApplicationMapper(),
            userId -> 0L,
            FIXED_CLOCK);

    DashboardResponse response = service.get(1L);

    assertEquals(1_917_500L, response.getCurrentAsset());
    assertEquals(1_100_000L, response.getCurrentSoldierSavingPrincipal());
    assertEquals(55_000L, response.getCurrentExpectedSavingInterest());
    assertEquals(1_100_000L, response.getCurrentGovernmentMatchingSupport());
    assertEquals(3_072_500L, response.getCurrentExpectedAsset());
  }

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  private static class LatestSimulationMapper implements SimulationMapper {
    private final SimulationVo simulation;

    private LatestSimulationMapper(SimulationVo simulation) {
      this.simulation = simulation;
    }

    @Override
    public int insert(SimulationVo simulation) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimulationVo findByIdAndUserId(long simulationId, long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<SimulationVo> findByUserId(long userId, int offset, int limit) {
      return List.of(simulation);
    }

    @Override
    public long countByUserId(long userId) {
      return 1L;
    }
  }

  private static class EmptyStrategyApplicationMapper implements StrategyApplicationMapper {
    @Override
    public int insert(StrategyApplicationVo strategyApplication) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int updateAfterExpectedAsset(long applicationId, long userId, long afterExpectedAsset) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findByIdAndUserId(long applicationId, long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findByAnalysisIdAndUserId(long analysisId, long userId) {
      return null;
    }

    @Override
    public StrategyApplicationVo findLatestByUserId(long userId) {
      return null;
    }

    @Override
    public StrategyApplicationVo findByGuidanceSelection(
        long userId,
        long guidanceId,
        InvestmentGuidanceAction action,
        InvestmentFrequency frequency,
        long contributionAmount) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationVo findLatestByGuidanceIdAndUserId(long guidanceId, long userId) {
      return null;
    }

    @Override
    public List<StrategyApplicationVo> findByUserId(long userId, long offset, int limit) {
      throw new UnsupportedOperationException();
    }
  }

  private static class RecordingCashflowService implements CashflowService {

    private CashflowCalculationInputResponse calculationInput =
        CashflowCalculationInputResponse.builder().baseAsset(1_500_000L).soldierSavings(List.of()).build();

    @Override
    public CashflowCalculationInputResponse getCalculationInput(long userId) {
      return calculationInput;
    }

    @Override
    public long getCurrentAsset(long userId) {
      return 1_500_000L;
    }

    private final CashflowForecastResponse generated;
    private int generateCount;

    private RecordingCashflowService(CashflowForecastResponse generated) {
      this.generated = generated;
    }

    @Override
    public CashflowForecastResponse generate(long userId) {
      generateCount++;
      return generated;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      throw new CashflowException(CashflowErrorCode.NOT_FOUND, "예측이 없습니다.");
    }

    @Override
    public CashflowForecastResponse getLatest(long userId, int months) {
      return getLatest(userId);
    }
  }
}
