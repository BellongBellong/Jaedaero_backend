package com.jaedaero.domain.simulation.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 시뮬레이션 계산에 필요한 도메인 입력값. */
public record SimulationInput(
    long baseAsset,
    long expectedSalary,
    long targetAmount,
    LocalDate dischargeDate,
    long mandatorySavingAmount,
    BigDecimal savingInterestRate,
    long governmentSupportExpected) {}
