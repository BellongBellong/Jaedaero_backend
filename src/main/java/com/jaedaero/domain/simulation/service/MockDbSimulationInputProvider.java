package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationInputMapper;
import com.jaedaero.domain.simulation.vo.SimulationInputSourceVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 현금흐름 API 연동 전까지 사용하는 개발용 제공자입니다.
 *
 * <p>목 DB의 연동 계좌 잔액과 목표·복무 정보를 읽어 가정 시뮬레이션 계산기에 전달합니다.
 */
@Component
@RequiredArgsConstructor
public class MockDbSimulationInputProvider implements SimulationInputProvider {

  private final SimulationInputMapper simulationInputMapper;

  @Override
  public SimulationInput load(long userId) {
    SimulationInputSourceVo source = simulationInputMapper.findLatestByUserId(userId);
    if (source == null
        || source.getBaseAsset() == null
        || source.getTargetAmount() == null
        || source.getSoldierType() == null
        || source.getEnlistmentDate() == null
        || source.getDischargeDate() == null) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY,
          "시뮬레이션에 필요한 캐시플로우, 목표 또는 복무 정보가 없습니다.");
    }

    try {
      return new SimulationInput(
          source.getBaseAsset(),
          source.getTargetAmount(),
          source.getMonthlySpendingAverage() == null ? 0L : source.getMonthlySpendingAverage(),
          SoldierType.valueOf(source.getSoldierType()),
          source.getEnlistmentDate(),
          source.getDischargeDate(),
          appliedStrategy(source));
    } catch (IllegalArgumentException exception) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY, "저장된 군종 정보가 올바르지 않습니다.");
    }
  }

  private AppliedCashflowStrategy appliedStrategy(SimulationInputSourceVo source) {
    if (source.getActiveStrategyApplicationId() == null) {
      return null;
    }
    if (source.getAppliedMonthlySpendingAmount() == null
        || source.getAppliedMonthlySavingAmount() == null
        || source.getAppliedMonthlyInvestmentAmount() == null
        || source.getAppliedExpectedReturnRate() == null) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY, "활성 AI 추천 전략의 배분 정보가 완전하지 않습니다.");
    }
    return new AppliedCashflowStrategy(
        source.getActiveStrategyApplicationId(),
        source.getAppliedMonthlySpendingAmount(),
        source.getAppliedMonthlySavingAmount(),
        source.getAppliedMonthlyInvestmentAmount(),
        source.getAppliedExpectedReturnRate());
  }

}
