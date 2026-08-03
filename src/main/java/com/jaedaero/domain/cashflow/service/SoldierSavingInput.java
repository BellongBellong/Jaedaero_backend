package com.jaedaero.domain.cashflow.service;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Current balance and terms of one synchronized soldier savings account. */
public record SoldierSavingInput(
    long currentBalance,
    long monthlyAmount,
    BigDecimal annualInterestRate,
    long governmentSupportExpected,
    LocalDate maturityDate) {}
