package com.jaedaero.domain.simulation.service.impl;

import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.SimulationCalculationResult;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import com.jaedaero.domain.simulation.service.SimulationInputProvider;
import com.jaedaero.domain.simulation.service.SimulationService;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.time.Clock;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class SimulationServiceImpl implements SimulationService {

  private static final DateTimeFormatter SCENARIO_DATE_FORMAT = DateTimeFormatter.ofPattern("yyyy.MM.dd");

  private final SimulationMapper simulationMapper;
  private final SimulationInputProvider simulationInputProvider;
  private final SimulationCalculator simulationCalculator;
  private final Clock clock;

  @Autowired
  public SimulationServiceImpl(
      SimulationMapper simulationMapper,
      SimulationInputProvider simulationInputProvider,
      SimulationCalculator simulationCalculator) {
    this(simulationMapper, simulationInputProvider, simulationCalculator, Clock.systemDefaultZone());
  }

  /** Constructor with a clock so date-dependent calculations can be tested deterministically. */
  public SimulationServiceImpl(
      SimulationMapper simulationMapper,
      SimulationInputProvider simulationInputProvider,
      SimulationCalculator simulationCalculator,
      Clock clock) {
    this.simulationMapper = simulationMapper;
    this.simulationInputProvider = simulationInputProvider;
    this.simulationCalculator = simulationCalculator;
    this.clock = clock;
  }

  @Override
  @Transactional
  public SimulationResponse run(long userId, SimulationRequest request) {
    LocalDate today = LocalDate.now(clock);
    SimulationInput input = simulationInputProvider.load(userId);
    SimulationCalculationResult result = simulationCalculator.calculate(input, request, today);
    boolean isSaved = request.getIsSaved() == null || request.getIsSaved();

    SimulationVo simulation =
        SimulationVo.builder()
            .userId(userId)
            .scenarioName(defaultScenarioName(request.getScenarioName(), today))
            .monthlySavingAmount(request.getMonthlySavingAmount())
            .investmentRatio(request.getInvestmentRatio())
            .expectedReturnRate(request.getExpectedReturnRate())
            .monthlySpendingAmount(request.getMonthlySpendingAmount())
            .expectedAsset(result.expectedAsset())
            .financialDischargeDate(result.financialDischargeDate())
            .isSaved(isSaved)
            .build();

    if (isSaved) {
      simulationMapper.insert(simulation);
    }
    return SimulationResponse.from(simulation);
  }

  @Override
  @Transactional(readOnly = true)
  public SimulationHistoryResponse getHistory(long userId, int page, int size) {
    long totalCount = simulationMapper.countByUserId(userId);
    List<SimulationResponse> simulations =
        simulationMapper.findByUserId(userId, page * size, size).stream()
            .map(SimulationResponse::from)
            .toList();
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
    return SimulationResponse.from(simulation);
  }

  private String defaultScenarioName(String scenarioName, LocalDate today) {
    if (scenarioName != null && !scenarioName.isBlank()) {
      return scenarioName.trim();
    }
    return "시뮬레이션 " + SCENARIO_DATE_FORMAT.format(today);
  }
}
