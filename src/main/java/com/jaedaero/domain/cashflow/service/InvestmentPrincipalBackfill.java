package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.YearMonth;
import org.springframework.stereotype.Component;

/** 증권계좌 미연동 사용자의 과거 투자원금을 계급별 월급 x 슬라이더 비율로 역산한다. */
@Component
public class InvestmentPrincipalBackfill {

  private final MilitaryPayPolicy militaryPayPolicy;

  public InvestmentPrincipalBackfill(MilitaryPayPolicy militaryPayPolicy) {
    this.militaryPayPolicy = militaryPayPolicy;
  }

  public long estimate(
      SoldierType soldierType,
      LocalDate enlistmentDate,
      LocalDate calculationDate,
      BigDecimal investmentRatio) {
    if (investmentRatio == null || investmentRatio.signum() == 0) {
      return 0L;
    }
    YearMonth enlistmentMonth = YearMonth.from(enlistmentDate);
    YearMonth calculationMonth = YearMonth.from(calculationDate);
    long total = 0L;
    for (YearMonth month = enlistmentMonth; month.isBefore(calculationMonth); month = month.plusMonths(1)) {
      long salary = militaryPayPolicy.resolve(soldierType, enlistmentMonth, month).monthlySalary();
      long backfilled =
          BigDecimal.valueOf(salary)
              .multiply(investmentRatio)
              .setScale(0, RoundingMode.HALF_UP)
              .longValueExact();
      total = Math.addExact(total, backfilled);
    }
    return total;
  }
}
