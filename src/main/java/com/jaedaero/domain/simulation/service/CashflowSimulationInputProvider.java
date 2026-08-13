package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.cashflow.dto.CashflowCalculationInputResponse;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 캐시플로우 도메인의 계산 입력 계약을 시뮬레이션 입력으로 변환합니다.
 *
 * <p>시뮬레이션은 목표·계좌·복무 테이블을 직접 조회하지 않습니다.
 */
@Component
@RequiredArgsConstructor
public class CashflowSimulationInputProvider implements SimulationInputProvider {

  private final CashflowService cashflowService;

  @Override
  public SimulationInput load(long userId) {
    try {
      CashflowCalculationInputResponse source = cashflowService.getCalculationInput(userId);
      return new SimulationInput(
          source.getUserId(),
          source.getBaseAsset(),
          source.getTargetAmount(),
          source.getMonthlySpendingAverage(),
          source.getSoldierType(),
          source.getEnlistmentDate(),
          source.getDischargeDate(),
          source.getSoldierSavings(),
          source.getAppliedStrategy());
    } catch (CashflowException exception) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY,
          "시뮬레이션에 필요한 캐시플로우, 목표 또는 복무 정보가 없습니다.");
    }
  }
}
