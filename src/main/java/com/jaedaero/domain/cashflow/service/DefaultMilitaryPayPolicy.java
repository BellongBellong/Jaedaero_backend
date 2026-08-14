package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper;
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import org.springframework.stereotype.Component;

/** 진급 일정에서 계급을, {@code military_pay_policy}에서 급여를 조회합니다. */
@Component
public class DefaultMilitaryPayPolicy implements MilitaryPayPolicy {

  public static final String POLICY_VERSION = "database-military-pay-policy";

  private final MilitaryPayPolicyMapper militaryPayPolicyMapper;

  public DefaultMilitaryPayPolicy(MilitaryPayPolicyMapper militaryPayPolicyMapper) {
    this.militaryPayPolicyMapper = militaryPayPolicyMapper;
  }

  @Override
  public MilitaryPay resolve(SoldierType soldierType, YearMonth enlistmentMonth, YearMonth month) {
    long elapsedMonths = ChronoUnit.MONTHS.between(enlistmentMonth, month);
    String rankName = rankName(soldierType, elapsedMonths);
    Long monthlySalary =
        militaryPayPolicyMapper.findMonthlySalary(
            soldierType.name(), rankName, month.atDay(1), month.atEndOfMonth());
    if (monthlySalary == null) {
      throw new CashflowException(
          CashflowErrorCode.PAY_POLICY_NOT_FOUND,
          String.format("%s %s의 %s년 %d월 봉급 정책이 없습니다.", soldierType, rankName, month.getYear(), month.getMonthValue()));
    }
    return new MilitaryPay(rankName, monthlySalary);
  }

  public static String rankName(SoldierType soldierType, long elapsedMonths) {
    long privateMonths =
        switch (soldierType) {
          case ARMY, NAVY, MARINE -> 2;
          case AIRFORCE -> 3;
        };
    if (elapsedMonths < privateMonths) return "이병";
    if (elapsedMonths < privateMonths + 6) return "일병";
    if (elapsedMonths < privateMonths + 12) return "상병";
    return "병장";
  }

  public record MilitaryPay(String rankName, long monthlySalary) {}
}
