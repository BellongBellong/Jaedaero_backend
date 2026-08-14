package com.jaedaero.domain.dashboard.service;

import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.cashflow.service.ConservativeMonthlyCashflowEngine;
import com.jaedaero.domain.dashboard.dto.DashboardResponse;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardSpendingMapper;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class DashboardServiceImpl implements DashboardService {

  private static final BigDecimal PERCENT = BigDecimal.valueOf(100);

  private final CashflowService cashflowService;
  private final DashboardMapper dashboardMapper;
  private final DashboardSpendingMapper dashboardSpendingMapper;
  private final StrategyApplicationMapper strategyApplicationMapper;
  private final Clock clock;
  private final SimulationMapper simulationMapper;

  @Autowired
  public DashboardServiceImpl(
      CashflowService cashflowService,
      DashboardMapper dashboardMapper,
      StrategyApplicationMapper strategyApplicationMapper,
      DashboardSpendingMapper dashboardSpendingMapper,
      SimulationMapper simulationMapper) {
    this(
        cashflowService,
        dashboardMapper,
        strategyApplicationMapper,
        dashboardSpendingMapper,
        simulationMapper,
        Clock.systemDefaultZone());
  }

  public DashboardServiceImpl(
      CashflowService cashflowService,
      DashboardMapper dashboardMapper,
      StrategyApplicationMapper strategyApplicationMapper,
      DashboardSpendingMapper dashboardSpendingMapper,
      Clock clock) {
    this(
        cashflowService,
        dashboardMapper,
        strategyApplicationMapper,
        dashboardSpendingMapper,
        null,
        clock);
  }

  public DashboardServiceImpl(
      CashflowService cashflowService,
      DashboardMapper dashboardMapper,
      StrategyApplicationMapper strategyApplicationMapper,
      DashboardSpendingMapper dashboardSpendingMapper,
      SimulationMapper simulationMapper,
      Clock clock) {
    this.cashflowService = cashflowService;
    this.dashboardMapper = dashboardMapper;
    this.strategyApplicationMapper = strategyApplicationMapper;
    this.dashboardSpendingMapper = dashboardSpendingMapper;
    this.simulationMapper = simulationMapper;
    this.clock = clock;
  }

  @Override
  public DashboardResponse get(long userId) {
    CashflowForecastResponse cashflow = latestOrGenerate(userId);
    CurrentAssetEstimate currentAssetEstimate =
        currentAssetEstimate(cashflowService.getCalculationInput(userId));
    return DashboardResponse.from(
        cashflow,
        currentAssetEstimate.currentAsset(),
        currentAssetEstimate.currentExpectedAsset(),
        currentAssetEstimate.soldierSavingPrincipal(),
        currentAssetEstimate.expectedSavingInterest(),
        currentAssetEstimate.governmentMatchingSupport(),
        dashboardMapper.findActualDischargeDateByUserId(userId),
        strategyApplicationMapper.findLatestByUserId(userId),
        latestSimulation(userId),
        LocalDate.now(clock),
        dashboardSpendingMapper.sumThisMonthSpendingByUserId(userId));
  }

  private CurrentAssetEstimate currentAssetEstimate(CashflowCalculationInputResponse input) {
    long soldierSavingPrincipal =
        input.getSoldierSavings().stream()
            .mapToLong(saving -> saving.currentBalance())
            .reduce(0L, Math::addExact);
    long expectedSavingInterest =
        rateAmount(
            soldierSavingPrincipal,
            ConservativeMonthlyCashflowEngine.SOLDIER_SAVING_ANNUAL_INTEREST_RATE);
    long governmentMatchingSupport =
        rateAmount(
            soldierSavingPrincipal, ConservativeMonthlyCashflowEngine.GOVERNMENT_MATCHING_RATE);
    long currentExpectedAsset =
        Math.addExact(
            input.getBaseAsset(), Math.addExact(expectedSavingInterest, governmentMatchingSupport));
    return new CurrentAssetEstimate(
        input.getBaseAsset(),
        currentExpectedAsset,
        soldierSavingPrincipal,
        expectedSavingInterest,
        governmentMatchingSupport);
  }

  private long rateAmount(long amount, BigDecimal ratePercent) {
    return BigDecimal.valueOf(amount)
        .multiply(ratePercent)
        .divide(PERCENT, 0, RoundingMode.HALF_UP)
        .longValueExact();
  }

  private SimulationVo latestSimulation(long userId) {
    if (simulationMapper == null) {
      return null;
    }
    return simulationMapper.findByUserId(userId, 0, 1).stream().findFirst().orElse(null);
  }

  private CashflowForecastResponse latestOrGenerate(long userId) {
    try {
      CashflowForecastResponse latest = cashflowService.getLatest(userId);
      if (!ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION.equals(
          latest.getCalculationPolicyVersion())) {
        return cashflowService.generate(userId);
      }
      return latest;
    } catch (CashflowException exception) {
      if (exception.getErrorCode() != CashflowErrorCode.NOT_FOUND) {
        throw exception;
      }
      return cashflowService.generate(userId);
    }
  }

  private record CurrentAssetEstimate(
      long currentAsset,
      long currentExpectedAsset,
      long soldierSavingPrincipal,
      long expectedSavingInterest,
      long governmentMatchingSupport) {}
}
