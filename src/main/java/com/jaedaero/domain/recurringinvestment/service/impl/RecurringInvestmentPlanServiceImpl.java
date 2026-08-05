package com.jaedaero.domain.recurringinvestment.service.impl;

import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanRequest;
import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanResponse;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanErrorCode;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanException;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentPlanService;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentSchedule;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanStatus;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.time.Clock;
import java.time.LocalDate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RecurringInvestmentPlanServiceImpl implements RecurringInvestmentPlanService {

  private final RecurringInvestmentPlanMapper mapper;
  private final Clock clock;

  public RecurringInvestmentPlanServiceImpl(
      RecurringInvestmentPlanMapper mapper, Clock clock) {
    this.mapper = mapper;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public RecurringInvestmentPlanResponse get(long userId) {
    return response(requiredPlan(userId));
  }

  @Override
  @Transactional
  public RecurringInvestmentPlanResponse save(
      long userId, RecurringInvestmentPlanRequest request) {
    validate(request);
    if (!mapper.existsSecuritiesAccount(request.getBrokerageAccountId(), userId)) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.INVALID_ACCOUNT,
          "본인 명의의 활성 증권 계좌만 적립 계획에 연결할 수 있습니다.");
    }

    RecurringInvestmentPlanVo existing = mapper.findByUserId(userId);
    RecurringInvestmentPlanStatus status =
        request.getContributionAmount() == 0
            ? RecurringInvestmentPlanStatus.PAUSED
            : RecurringInvestmentPlanStatus.ACTIVE;
    RecurringInvestmentPlanVo plan =
        RecurringInvestmentPlanVo.builder()
            .planId(existing == null ? null : existing.getPlanId())
            .userId(userId)
            .brokerageAccountId(request.getBrokerageAccountId())
            .frequency(request.getFrequency())
            .contributionDay(request.getContributionDay())
            .contributionAmount(request.getContributionAmount())
            .maximumMonthlyAmount(request.getMaximumMonthlyAmount())
            .investmentProductCode(request.getInvestmentProductCode().trim())
            .investmentProductName(request.getInvestmentProductName().trim())
            .status(status)
            .nextContributionDate(
                RecurringInvestmentSchedule.nextDate(
                    LocalDate.now(clock), request.getFrequency(), request.getContributionDay()))
            .build();
    if (existing == null) {
      mapper.insert(plan);
    } else {
      mapper.update(plan);
    }
    return response(requiredPlan(userId));
  }

  private RecurringInvestmentPlanVo requiredPlan(long userId) {
    RecurringInvestmentPlanVo plan = mapper.findByUserId(userId);
    if (plan == null) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.NOT_FOUND, "설정된 적립식 투자 계획이 없습니다.");
    }
    return plan;
  }

  private void validate(RecurringInvestmentPlanRequest request) {
    InvestmentFrequency frequency = request.getFrequency();
    int day = request.getContributionDay();
    if ((frequency == InvestmentFrequency.WEEKLY && day > 7)
        || (frequency == InvestmentFrequency.MONTHLY && day > 28)) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.INVALID_PLAN,
          "주간 적립일은 1~7, 월간 적립일은 1~28 사이여야 합니다.");
    }
    long monthlyEquivalent =
        RecurringInvestmentSchedule.monthlyEquivalent(
            frequency, request.getContributionAmount());
    if (monthlyEquivalent > request.getMaximumMonthlyAmount()) {
      throw new RecurringInvestmentPlanException(
          RecurringInvestmentPlanErrorCode.INVALID_PLAN,
          "현재 적립금의 월 환산액은 월 최대 투자한도를 넘을 수 없습니다.");
    }
  }

  private RecurringInvestmentPlanResponse response(RecurringInvestmentPlanVo plan) {
    return RecurringInvestmentPlanResponse.from(
        plan,
        RecurringInvestmentSchedule.monthlyEquivalent(
            plan.getFrequency(), plan.getContributionAmount()));
  }
}
