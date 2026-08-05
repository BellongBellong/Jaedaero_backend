package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.time.LocalDate;

public record InvestmentGuidanceCalculationInput(
    long baselineExpectedAsset,
    long targetAmount,
    String rankName,
    LocalDate dischargeDate,
    BigDecimal expectedReturnRate,
    RecurringInvestmentPlanVo plan) {}
