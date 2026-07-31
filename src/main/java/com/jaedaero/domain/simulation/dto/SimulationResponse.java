package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class SimulationResponse {

  private final Long simulationId;
  private final String scenarioName;
  private final Long monthlySpendingAmount;
  private final Long monthlySavingAmount;
  private final BigDecimal investmentRatio;
  private final BigDecimal expectedReturnRate;
  private final Long expectedAsset;
  private final LocalDate financialDischargeDate;
  private final Boolean isSaved;

  public static SimulationResponse from(SimulationVo simulation) {
    return SimulationResponse.builder()
        .simulationId(simulation.getSimulationId())
        .scenarioName(simulation.getScenarioName())
        .monthlySpendingAmount(simulation.getMonthlySpendingAmount())
        .monthlySavingAmount(simulation.getMonthlySavingAmount())
        .investmentRatio(simulation.getInvestmentRatio())
        .expectedReturnRate(simulation.getExpectedReturnRate())
        .expectedAsset(simulation.getExpectedAsset())
        .financialDischargeDate(simulation.getFinancialDischargeDate())
        .isSaved(simulation.getIsSaved())
        .build();
  }
}
