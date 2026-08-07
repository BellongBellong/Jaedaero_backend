package com.jaedaero.domain.simulation.dto;

import com.jaedaero.domain.simulation.vo.SimulationVo;
import lombok.Builder;
import lombok.Getter;

/** 전역 예상자산의 보수적 현금흐름과 월 배분 원금을 설명하는 상세 응답이다. */
@Getter
@Builder
public class SimulationCalculationDetailResponse {

  private final Integer calculationMonths;
  private final Long baseAsset;
  private final Long expectedSalary;
  private final Long expectedSpending;
  private final Long cashflowIncreaseAmount;
  private final Long soldierSavingPrincipal;
  private final Long investmentPrincipal;
  private final Long unallocatedPrincipal;

  public static SimulationCalculationDetailResponse from(SimulationVo simulation) {
    if (simulation.getCalculationMonths() == null) {
      return null;
    }
    return builder()
        .calculationMonths(simulation.getCalculationMonths())
        .baseAsset(simulation.getBaseAsset())
        .expectedSalary(simulation.getExpectedSalary())
        .expectedSpending(simulation.getExpectedSpending())
        .cashflowIncreaseAmount(
            Math.subtractExact(simulation.getExpectedSalary(), simulation.getExpectedSpending()))
        .soldierSavingPrincipal(simulation.getSoldierSavingPrincipal())
        .investmentPrincipal(simulation.getInvestmentPrincipal())
        .unallocatedPrincipal(simulation.getUnallocatedPrincipal())
        .build();
  }
}
