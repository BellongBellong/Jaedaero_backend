package com.jaedaero.domain.goal.service.impl;

import com.jaedaero.domain.goal.dto.GoalRequest;
import com.jaedaero.domain.goal.dto.GoalResponse;
import com.jaedaero.domain.goal.mapper.GoalMapper;
import com.jaedaero.domain.goal.service.GoalService;
import com.jaedaero.domain.mypage.exception.MyPageErrorCode;
import com.jaedaero.domain.mypage.exception.MyPageException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class GoalServiceImpl implements GoalService {
  private final GoalMapper goalMapper;

  /** 사용자의 투자 목표 금액을 변경합니다. */
  @Override
  @Transactional
  public GoalResponse updateGoal(long userId, GoalRequest request) {
    if (goalMapper.updateTargetAmount(userId, request.getTargetAmount()) == 0) {
      throw new MyPageException(MyPageErrorCode.USER_NOT_FOUND, "목표 정보를 찾을 수 없습니다.");
    }
    return new GoalResponse(request.getTargetAmount());
  }
}
