package com.jaedaero.domain.investmentguidance.dto;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
@AllArgsConstructor
public class InvestmentGuidanceApplyRequest {

  @NotNull private InvestmentGuidanceAction selectedAction;
  @NotNull @Min(0) private Long selectedContributionAmount;
}
