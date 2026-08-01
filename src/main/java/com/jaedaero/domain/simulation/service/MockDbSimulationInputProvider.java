package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.simulation.exception.SimulationErrorCode;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.mapper.SimulationInputMapper;
import com.jaedaero.domain.simulation.vo.SimulationInputSourceVo;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

/**
 * 팀 캐시플로우 API 연동 전까지 사용하는 개발용 Provider.
 *
 * <p>최신 {@code cashflow_forecast}와 목표·전역일·장병적금 정보를 읽는다. 실제 연동이 준비되면 이
 * 구현체를 캐시플로우 Service/DTO 기반 구현체로 교체한다.
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
        || source.getExpectedSalary() == null
        || source.getTargetAmount() == null
        || source.getDischargeDate() == null) {
      throw new SimulationException(
          SimulationErrorCode.INPUT_NOT_READY,
          "시뮬레이션에 필요한 캐시플로우, 목표 또는 복무 정보가 없습니다.");
    }

    return new SimulationInput(
        source.getBaseAsset(),
        source.getExpectedSalary(),
        source.getTargetAmount(),
        source.getDischargeDate(),
        defaultIfNull(source.getMandatorySavingAmount()),
        source.getSavingInterestRate() == null ? BigDecimal.ZERO : source.getSavingInterestRate(),
        defaultIfNull(source.getGovernmentSupportExpected()));
  }

  private long defaultIfNull(Long value) {
    return value == null ? 0L : value;
  }
}
