package com.jaedaero.domain.simulation.service;

/** 캐시플로우·목표 도메인으로부터 시뮬레이션 입력을 공급한다. */
public interface SimulationInputProvider {

  SimulationInput load(long userId);
}
