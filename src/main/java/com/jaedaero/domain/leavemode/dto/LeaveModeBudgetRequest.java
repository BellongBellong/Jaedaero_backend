package com.jaedaero.domain.leavemode.dto;

import javax.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveModeBudgetRequest {

  @PositiveOrZero(message = "휴가 예산은 0 이상이어야 합니다.")
  private Long budgetAmount;
}
