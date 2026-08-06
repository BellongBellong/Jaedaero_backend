package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.service.SimulationAllocationMetrics;
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
  private final Long targetAmount;
  private final Long monthlySpendingAmount;
  private final Long monthlySavingAmount;
  private final Long monthlyInvestmentAmount;
  private final Long referenceMonthlyIncome;
  private final BigDecimal spendingRate;
  private final BigDecimal savingRate;
  private final BigDecimal investmentRate;
  private final Long unallocatedAmount;
  private final BigDecimal expectedReturnRate;
  private final Long expectedAsset;
  private final LocalDate financialDischargeDate;
  private final SimulationCalculationDetailResponse calculationDetail;
  private final SimulationExpectedEffectResponse expectedEffect;
  private final Boolean isSaved;

  public static SimulationResponse from(
      SimulationVo simulation, SimulationAllocationMetrics allocationMetrics) {
    return SimulationResponse.builder()
        .simulationId(simulation.getSimulationId())
        .scenarioName(simulation.getScenarioName())
        .targetAmount(simulation.getTargetAmount())
        .monthlySpendingAmount(simulation.getMonthlySpendingAmount())
        .monthlySavingAmount(simulation.getMonthlySavingAmount())
        .monthlyInvestmentAmount(simulation.getMonthlyInvestmentAmount())
        .referenceMonthlyIncome(allocationMetrics.referenceMonthlyIncome())
        .spendingRate(allocationMetrics.spendingRate())
        .savingRate(allocationMetrics.savingRate())
        .investmentRate(allocationMetrics.investmentRate())
        .unallocatedAmount(allocationMetrics.unallocatedAmount())
        .expectedReturnRate(simulation.getExpectedReturnRate())
        .expectedAsset(simulation.getExpectedAsset())
        .financialDischargeDate(simulation.getFinancialDischargeDate())
        .calculationDetail(SimulationCalculationDetailResponse.from(simulation))
        .expectedEffect(SimulationExpectedEffectResponse.from(simulation))
        .isSaved(simulation.getIsSaved())
        .build();
  }
}
