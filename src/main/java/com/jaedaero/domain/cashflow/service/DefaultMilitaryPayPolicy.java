package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/**
 * Temporary 2026 basic-pay policy. The next step is to read the same values from
 * {@code military_pay_policy} once yearly policy data is managed in the database.
 */
@Component
public class DefaultMilitaryPayPolicy {

  public static final String POLICY_VERSION = "2026-basic-pay";

  public MilitaryPay resolve(SoldierType soldierType, YearMonth enlistmentMonth, YearMonth month) {
    long elapsedMonths = ChronoUnit.MONTHS.between(enlistmentMonth, month);
    if (elapsedMonths < 2) return new MilitaryPay("PRIVATE", 750_000L);
    if (elapsedMonths < 8) return new MilitaryPay("PRIVATE_FIRST_CLASS", 900_000L);
    if (elapsedMonths < 14) return new MilitaryPay("CORPORAL", 1_200_000L);
    return new MilitaryPay("SERGEANT", 1_500_000L);
  }

  public record MilitaryPay(String rankName, long monthlySalary) {}
}
