package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** 군적금·투자 이자·수익 참고 정보를 원금과 분리해 제공한다. */
@Getter
@Builder
public class SimulationExpectedEffectResponse {

  private final BigDecimal soldierSavingAnnualInterestRate;
  private final BigDecimal governmentMatchingRate;
  private final BigDecimal investmentAnnualReturnRate;
  private final String compoundingPeriod;
  private final Long soldierSavingInterest;
  private final Long governmentMatchingSupport;
  private final Long expectedInvestmentReturn;
  private final Long projectedBenefitAmount;
  private final String calculationPolicyVersion;

  public static SimulationExpectedEffectResponse from(SimulationVo simulation) {
    if (simulation.getCalculationMonths() == null) {
      return null;
    }
    long projectedBenefit =
        Math.addExact(
            Math.addExact(
                simulation.getSoldierSavingInterest(),
                simulation.getGovernmentMatchingSupport()),
            simulation.getExpectedInvestmentReturn());
    return builder()
        .soldierSavingAnnualInterestRate(
            SimulationCalculator.SOLDIER_SAVING_ANNUAL_INTEREST_RATE)
        .governmentMatchingRate(SimulationCalculator.GOVERNMENT_MATCHING_RATE)
        .investmentAnnualReturnRate(simulation.getExpectedReturnRate())
        .compoundingPeriod("MONTHLY_END_OF_PERIOD")
        .soldierSavingInterest(simulation.getSoldierSavingInterest())
        .governmentMatchingSupport(simulation.getGovernmentMatchingSupport())
        .expectedInvestmentReturn(simulation.getExpectedInvestmentReturn())
        .projectedBenefitAmount(projectedBenefit)
        .calculationPolicyVersion(simulation.getCalculationPolicyVersion())
        .build();
  }
}
