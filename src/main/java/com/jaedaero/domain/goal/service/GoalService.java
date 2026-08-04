package com.jaedaero.domain.goal.service;

import com.jaedaero.domain.goal.dto.GoalRequest;
import com.jaedaero.domain.goal.dto.GoalResponse;

public interface GoalService {

  /** 사용자의 목표 금액을 변경합니다. */
  GoalResponse updateGoal(long userId, GoalRequest request);
}
