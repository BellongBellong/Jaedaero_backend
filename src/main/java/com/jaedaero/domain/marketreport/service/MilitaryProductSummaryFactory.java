package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MilitaryProductSummaryItem;
import com.jaedaero.domain.simulation.service.SimulationAllocationPolicy;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import org.springframework.stereotype.Component;

/** 별도 외부 호출 없이 확정된 장병내일준비적금 정책을 카드로 제공한다. */
@Component
public class MilitaryProductSummaryFactory {

  public MilitaryProductSummaryItem create() {
    return MilitaryProductSummaryItem.builder()
        .annualInterestRate(SimulationCalculator.SOLDIER_SAVING_ANNUAL_INTEREST_RATE)
        .governmentMatchingRate(SimulationCalculator.GOVERNMENT_MATCHING_RATE)
        .maxMonthlySavingAmount(SimulationAllocationPolicy.MAX_MONTHLY_SAVING_AMOUNT)
        .build();
  }
}
