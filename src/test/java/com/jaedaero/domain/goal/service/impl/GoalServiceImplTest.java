package com.jaedaero.domain.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.goal.dto.GoalRequest;
import com.jaedaero.domain.goal.dto.GoalResponse;
import com.jaedaero.domain.goal.mapper.GoalMapper;
import com.jaedaero.domain.mypage.exception.MyPageException;
import org.junit.jupiter.api.Test;

class GoalServiceImplTest {

  @Test
  void updatesTargetAmount() {
    StubGoalMapper mapper = new StubGoalMapper();
    GoalServiceImpl service = new GoalServiceImpl(mapper);
    GoalRequest request = new GoalRequest();
    request.setTargetAmount(10_000_000L);

    GoalResponse response = service.updateGoal(1L, request);

    assertEquals(10_000_000L, response.getTargetAmount());
    assertEquals(10_000_000L, mapper.updatedTargetAmount);
  }

  @Test
  void throwsWhenGoalDoesNotExist() {
    StubGoalMapper mapper = new StubGoalMapper();
    mapper.goalExists = false;
    GoalServiceImpl service = new GoalServiceImpl(mapper);
    GoalRequest request = new GoalRequest();
    request.setTargetAmount(10_000_000L);

    assertThrows(MyPageException.class, () -> service.updateGoal(1L, request));
  }

  private static class StubGoalMapper implements GoalMapper {
    private boolean goalExists = true;
    private long updatedTargetAmount;

    @Override public Long findTargetAmount(long userId) { return null; }
    @Override public int updateTargetAmount(long userId, long targetAmount) {
      updatedTargetAmount = targetAmount;
      return goalExists ? 1 : 0;
    }
  }
}
