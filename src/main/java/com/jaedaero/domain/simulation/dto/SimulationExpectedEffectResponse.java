package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import lombok.Builder;
import lombok.Getter;

/** 수익 가정에 따른 참고 효과와 보수적 예상자산의 관계를 명시한다. */
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
  private final Boolean returnsIncludedInExpectedAsset;
  private final Long conservativeExpectedAsset;
  private final Long potentialExpectedAsset;
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
        .returnsIncludedInExpectedAsset(false)
        .conservativeExpectedAsset(simulation.getExpectedAsset())
        .potentialExpectedAsset(simulation.getPotentialExpectedAsset())
        .calculationPolicyVersion(simulation.getCalculationPolicyVersion())
        .build();
  }
}
