package com.jaedaero.domain.cashflow.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** 동기화한 장병 적금 계좌 한 건의 현재 잔액과 조건입니다. */
public record SoldierSavingInput(
    long currentBalance,
    long monthlyAmount,
    BigDecimal annualInterestRate,
    long governmentSupportExpected,
    LocalDate maturityDate) {}
