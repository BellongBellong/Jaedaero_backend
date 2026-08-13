package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.exception.CashflowErrorCode;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.CashflowMapper;
import com.jaedaero.domain.cashflow.vo.CashflowInputSourceVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class DbCashflowInputProvider implements CashflowInputProvider {

  private final CashflowMapper cashflowMapper;

  @Override
  public CashflowInput load(long userId) {
    CashflowInputSourceVo source = cashflowMapper.findInputByUserId(userId);
    if (source == null
        || source.getTargetAmount() == null
        || source.getSoldierType() == null
        || source.getEnlistmentDate() == null
        || source.getDischargeDate() == null) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "캐시플로우 계산에 필요한 군 복무 정보 또는 목표 금액이 없습니다.");
    }
    try {
      return new CashflowInput(
          defaultIfNull(source.getBaseAsset()),
          source.getTargetAmount(),
          defaultIfNull(source.getMonthlySpendingAverage()),
          SoldierType.valueOf(source.getSoldierType()),
          source.getEnlistmentDate(),
          source.getDischargeDate(),
          cashflowMapper.findSoldierSavingsByUserId(userId).stream()
              .map(
                  saving ->
                      new SoldierSavingInput(
                          defaultIfNull(saving.getCurrentBalance()),
                          saving.getMonthlyAmount(),
                          saving.getInterestRate(),
                          defaultIfNull(saving.getGovernmentSupportExpected()),
                          saving.getStartDate(),
                          saving.getEndDate()))
              .toList(),
          appliedStrategy(source));
    } catch (IllegalArgumentException exception) {
      throw new CashflowException(CashflowErrorCode.INPUT_NOT_READY, "저장된 군종 정보가 올바르지 않습니다.");
    }
  }

  private long defaultIfNull(Long value) {
    return value == null ? 0L : value;
  }

  private AppliedCashflowStrategy appliedStrategy(CashflowInputSourceVo source) {
    if (source.getActiveStrategyApplicationId() == null) {
      return null;
    }
    if (source.getAppliedMonthlySpendingAmount() == null
        || source.getAppliedMonthlySavingAmount() == null
        || source.getAppliedMonthlyInvestmentAmount() == null
        || source.getAppliedExpectedReturnRate() == null) {
      throw new CashflowException(
          CashflowErrorCode.INPUT_NOT_READY, "활성 AI 전략의 월 배분 정보가 완전하지 않습니다.");
    }
    return new AppliedCashflowStrategy(
        source.getActiveStrategyApplicationId(),
        source.getAppliedMonthlySpendingAmount(),
        source.getAppliedMonthlySavingAmount(),
        source.getAppliedMonthlyInvestmentAmount(),
        source.getAppliedExpectedReturnRate());
  }
}
