package com.jaedaero.domain.simulation.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SimulationVo {

  private Long simulationId;
  private Long userId;
  private String scenarioName;
  private Long targetAmount;
  private Long monthlySavingAmount;
  private Long monthlyInvestmentAmount;
  private BigDecimal expectedReturnRate;
  private Long monthlySpendingAmount;
  private Long expectedAsset;
  private LocalDate financialDischargeDate;
  private Integer calculationMonths;
  private Long baseAsset;
  private Long expectedSalary;
  private Long expectedSpending;
  private Long soldierSavingPrincipal;
  private Long soldierSavingInterest;
  private Long governmentMatchingSupport;
  private Long investmentPrincipal;
  private Long expectedInvestmentReturn;
  private Long unallocatedPrincipal;
  private String calculationPolicyVersion;
  private Boolean isSaved;
  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;
}
