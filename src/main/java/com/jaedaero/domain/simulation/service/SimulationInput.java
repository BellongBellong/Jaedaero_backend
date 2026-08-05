package com.jaedaero.domain.simulation.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.LocalDate;

/** 시뮬레이션 계산에 필요한 도메인 입력값. */
public record SimulationInput(
    long baseAsset,
    long targetAmount,
    SoldierType soldierType,
    LocalDate enlistmentDate,
    LocalDate dischargeDate) {}
