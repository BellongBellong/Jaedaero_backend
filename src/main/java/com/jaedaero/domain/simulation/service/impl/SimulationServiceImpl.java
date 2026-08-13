package com.jaedaero.domain.simulation.service.impl;

import com.jaedaero.domain.simulation.dto.SimulationDefaultsResponse;
import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.SimulationAllocationMetrics;
import com.jaedaero.domain.simulation.service.SimulationAllocationPolicy;
import com.jaedaero.domain.simulation.service.SimulationCalculationResult;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import com.jaedaero.domain.simulation.service.SimulationInputProvider;
import com.jaedaero.domain.simulation.service.SimulationService;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationServiceImpl implements SimulationService {

  private static final DateTimeFormatter SCENARIO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final SimulationMapper simulationMapper;
  private final SimulationInputProvider simulationInputProvider;
  private final SimulationCalculator simulationCalculator;
  private final SimulationAllocationPolicy allocationPolicy;
  private final Clock clock;

  public SimulationServiceImpl(
      SimulationMapper simulationMapper,
      SimulationInputProvider simulationInputProvider,
      SimulationCalculator simulationCalculator,
      SimulationAllocationPolicy allocationPolicy,
      Clock clock) {
    this.simulationMapper = simulationMapper;
    this.simulationInputProvider = simulationInputProvider;
    this.simulationCalculator = simulationCalculator;
    this.allocationPolicy = allocationPolicy;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationDefaultsResponse getDefaults(long userId) {
    LocalDate today = LocalDate.now(clock);
    SimulationInput input = simulationInputProvider.load(userId);
    long referenceMonthlyIncome = simulationCalculator.referenceMonthlyIncome(input, today);
    long monthlySpendingAmount;
    long monthlySavingAmount;
    long monthlyInvestmentAmount;
    BigDecimal expectedReturnRate;
    if (input.appliedStrategy() == null) {
      monthlySavingAmount =
          Math.min(
              SimulationAllocationPolicy.MAX_MONTHLY_SAVING_AMOUNT, referenceMonthlyIncome);
      monthlyInvestmentAmount = 0L;
      monthlySpendingAmount = 0L;
      expectedReturnRate = SimulationAllocationPolicy.DEFAULT_EXPECTED_RETURN_RATE;
    } else {
      monthlySpendingAmount = input.appliedStrategy().monthlySpendingAmount();
      monthlySavingAmount = input.appliedStrategy().monthlySavingAmount();
      monthlyInvestmentAmount = input.appliedStrategy().monthlyInvestmentAmount();
      expectedReturnRate = input.appliedStrategy().expectedReturnRate();
      allocationPolicy.validate(
          monthlySpendingAmount,
          monthlySavingAmount,
          monthlyInvestmentAmount,
          expectedReturnRate,
          referenceMonthlyIncome);
    }
    SimulationAllocationMetrics metrics =
        allocationPolicy.metrics(
            monthlySpendingAmount,
            monthlySavingAmount,
            monthlyInvestmentAmount,
            referenceMonthlyIncome);
    return SimulationDefaultsResponse.of(
        input.targetAmount(),
        monthlySpendingAmount,
        monthlySavingAmount,
        monthlyInvestmentAmount,
        expectedReturnRate,
        SimulationAllocationPolicy.MAX_MONTHLY_SAVING_AMOUNT,
        metrics);
  }

  @Override
  @Transactional
  public SimulationResponse run(long userId, SimulationRequest request) {
    LocalDate today = LocalDate.now(clock);
    SimulationInput input = simulationInputProvider.load(userId);
    long referenceMonthlyIncome = simulationCalculator.referenceMonthlyIncome(input, today);
    allocationPolicy.validate(request, referenceMonthlyIncome);
    SimulationCalculationResult result = simulationCalculator.calculate(input, request, today);
    boolean isSaved = request.getIsSaved() == null || request.getIsSaved();

    SimulationVo simulation =
        SimulationVo.builder()
            .userId(userId)
            .scenarioName(defaultScenarioName(request.getScenarioName(), today))
            .targetAmount(input.targetAmount())
            .monthlySavingAmount(request.getMonthlySavingAmount())
            .monthlyInvestmentAmount(request.getMonthlyInvestmentAmount())
            .expectedReturnRate(request.getExpectedReturnRate())
            .monthlySpendingAmount(request.getMonthlySpendingAmount())
            .expectedAsset(result.expectedAsset())
            .financialDischargeDate(result.financialDischargeDate())
            .calculationMonths(result.calculationMonths())
            .baseAsset(result.baseAsset())
            .expectedSalary(result.expectedSalary())
            .expectedSpending(result.expectedSpending())
            .soldierSavingPrincipal(result.soldierSavingPrincipal())
            .soldierSavingInterest(result.soldierSavingInterest())
            .governmentMatchingSupport(result.governmentMatchingSupport())
            .investmentPrincipal(result.investmentPrincipal())
            .expectedInvestmentReturn(result.expectedInvestmentReturn())
            .unallocatedPrincipal(result.unallocatedPrincipal())
            .potentialExpectedAsset(result.potentialExpectedAsset())
            .calculationPolicyVersion(result.calculationPolicyVersion())
            .isSaved(isSaved)
            .build();

    if (isSaved) {
      simulationMapper.insert(simulation);
    }
    return response(simulation, referenceMonthlyIncome);
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationHistoryResponse getHistory(long userId, int page, int size) {
    long totalCount = simulationMapper.countByUserId(userId);
    List<SimulationVo> savedSimulations =
        simulationMapper.findByUserId(userId, page * size, size);
    List<SimulationResponse> simulations;
    if (savedSimulations.isEmpty()) {
      simulations = List.of();
    } else {
      SimulationInput input = simulationInputProvider.load(userId);
      simulations = savedSimulations.stream().map(simulation -> response(simulation, input)).toList();
    }
    return SimulationHistoryResponse.builder()
        .simulations(simulations)
        .page(page)
        .size(size)
        .totalCount(totalCount)
        .hasNext((long) (page + 1) * size < totalCount)
        .build();
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationResponse getDetail(long userId, long simulationId) {
    SimulationVo simulation = simulationMapper.findByIdAndUserId(simulationId, userId);
    if (simulation == null) {
      throw new SimulationException(SimulationErrorCode.NOT_FOUND, "시뮬레이션 이력을 찾을 수 없습니다.");
    }
    return response(simulation, simulationInputProvider.load(userId));
  }

  private SimulationResponse response(SimulationVo simulation, SimulationInput input) {
    LocalDate calculationDate =
        simulation.getCreatedAt() == null
            ? LocalDate.now(clock)
            : simulation.getCreatedAt().toLocalDate();
    return response(
        simulation, simulationCalculator.referenceMonthlyIncome(input, calculationDate));
  }

  private SimulationResponse response(SimulationVo simulation, long referenceMonthlyIncome) {
    SimulationAllocationMetrics metrics =
        allocationPolicy.metrics(
            simulation.getMonthlySpendingAmount(),
            simulation.getMonthlySavingAmount(),
            simulation.getMonthlyInvestmentAmount(),
            referenceMonthlyIncome);
    return SimulationResponse.from(simulation, metrics);
  }

  private String defaultScenarioName(String scenarioName, LocalDate today) {
    if (scenarioName != null && !scenarioName.isBlank()) {
      return scenarioName.trim();
    }
    return "시뮬레이션 " + SCENARIO_DATE_FORMAT.format(today);
  }
}
