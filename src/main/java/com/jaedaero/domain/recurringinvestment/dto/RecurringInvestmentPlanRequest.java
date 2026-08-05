package com.jaedaero.domain.recurringinvestment.dto;

import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class RecurringInvestmentPlanRequest {

  @NotNull private InvestmentFrequency frequency;
  @NotNull @Min(1) private Integer contributionDay;
  @NotNull @Min(0) private Long contributionAmount;
  @NotNull @Min(0) private Long maximumMonthlyAmount;
  @NotNull @Min(1) private Long brokerageAccountId;

  @NotBlank
  @Size(max = 100)
  private String investmentProductCode;

  @NotBlank
  @Size(max = 255)
  private String investmentProductName;
}
