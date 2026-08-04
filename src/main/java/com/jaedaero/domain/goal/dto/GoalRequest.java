package com.jaedaero.domain.goal.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class GoalRequest {

  @NotNull @PositiveOrZero private Long targetAmount;
}
