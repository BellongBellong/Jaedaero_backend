package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationInputMapper;
import com.jaedaero.domain.simulation.vo.SimulationInputSourceVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 캐시플로우 API 연동 전까지 사용하는 개발용 Provider.
 *
 * <p>MockDB의 연동 계좌 잔액과 목표·복무 정보를 읽어 What-if 계산기에 전달한다.
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
          SoldierType.valueOf(source.getSoldierType()),
          source.getEnlistmentDate(),
          source.getDischargeDate());
    } catch (IllegalArgumentException exception) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY, "저장된 군종 정보가 올바르지 않습니다.");
    }
  }

}
