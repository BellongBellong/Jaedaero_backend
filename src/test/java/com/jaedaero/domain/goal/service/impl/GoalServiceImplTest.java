package com.jaedaero.domain.goal.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.goal.dto.GoalRequest;
import com.jaedaero.domain.goal.dto.GoalResponse;
import com.jaedaero.domain.goal.mapper.GoalMapper;
import com.jaedaero.domain.mypage.exception.MyPageException;
import org.junit.jupiter.api.Test;

class GoalServiceImplTest {

  @Test
  void updatesTargetAmount() {
    StubGoalMapper mapper = new StubGoalMapper();
    RecordingCashflowService cashflowService = new RecordingCashflowService();
    GoalServiceImpl service = new GoalServiceImpl(mapper, cashflowService);
    GoalRequest request = new GoalRequest();
    request.setTargetAmount(10_000_000L);

    GoalResponse response = service.updateGoal(1L, request);

    assertEquals(10_000_000L, response.getTargetAmount());
    assertEquals(10_000_000L, mapper.updatedTargetAmount);
    assertEquals(1L, cashflowService.generatedUserId);
  }

  @Test
  void returnsCurrentTargetAmount() {
    StubGoalMapper mapper = new StubGoalMapper();
    mapper.targetAmount = 20_000_000L;
    GoalServiceImpl service = new GoalServiceImpl(mapper, new RecordingCashflowService());

    GoalResponse response = service.getGoal(1L);

    assertEquals(20_000_000L, response.getTargetAmount());
  }

  @Test
  void throwsWhenGoalDoesNotExist() {
    StubGoalMapper mapper = new StubGoalMapper();
    mapper.goalExists = false;
    RecordingCashflowService cashflowService = new RecordingCashflowService();
    GoalServiceImpl service = new GoalServiceImpl(mapper, cashflowService);
    GoalRequest request = new GoalRequest();
    request.setTargetAmount(10_000_000L);

    assertThrows(MyPageException.class, () -> service.updateGoal(1L, request));
    assertEquals(0L, cashflowService.generatedUserId);
  }

  private static class StubGoalMapper implements GoalMapper {
    private boolean goalExists = true;
    private Long targetAmount;
    private long updatedTargetAmount;

    @Override public Long findTargetAmount(long userId) { return targetAmount; }
    @Override public int updateTargetAmount(long userId, long targetAmount) {
      updatedTargetAmount = targetAmount;
      return goalExists ? 1 : 0;
    }
  }

  private static class RecordingCashflowService implements CashflowService {
    private long generatedUserId;

    @Override
    public com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse getCalculationInput(
        long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CashflowForecastResponse generate(long userId) {
      generatedUserId = userId;
      return null;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      return null;
    }

    @Override
    public CashflowForecastResponse getLatest(long userId, int months) {
      return null;
    }
  }
}
