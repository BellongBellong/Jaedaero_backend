package com.jaedaero.domain.investmentguidance.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceErrorCode;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceException;
import com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceMapper;
import com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceInputMapper;
import com.jaedaero.domain.investmentguidance.service.impl.InvestmentGuidanceServiceImpl;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceInputSourceVo;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceVo;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanStatus;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Objects;
import org.junit.jupiter.api.Test;

class InvestmentGuidanceServiceImplTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void createsSafeFocusGuidanceAndAppliesSameSelectionIdempotently() {
    InMemoryGuidanceMapper guidanceMapper = new InMemoryGuidanceMapper();
    InMemoryPlanMapper planMapper = new InMemoryPlanMapper();
    InMemoryApplicationMapper applicationMapper = new InMemoryApplicationMapper();
    InvestmentGuidanceService service =
        new InvestmentGuidanceServiceImpl(
            guidanceMapper,
            new StubGuidanceInputMapper(),
            planMapper,
            applicationMapper,
            (userId, plan) ->
                new BrokeragePositionSnapshot(
                    1_100_000L,
                    1_000_000L,
                    1_100_000L,
                    100_000L,
                    new BigDecimal("10.0000"),
                    LocalDateTime.of(2026, 8, 4, 9, 0)),
            new InvestmentGuidanceCalculator(),
            new TemplateInvestmentGuidanceReasonGenerator(),
            FIXED_CLOCK);

    InvestmentGuidanceResponse guidance = service.create(1L);
    InvestmentGuidanceApplyRequest request =
        new InvestmentGuidanceApplyRequest(InvestmentGuidanceAction.SAFE_FOCUS, 0L);
    StrategyApplicationResponse first = service.apply(1L, guidance.getGuidanceId(), request);
    StrategyApplicationResponse duplicate = service.apply(1L, guidance.getGuidanceId(), request);

    assertEquals(InvestmentGuidanceAction.SAFE_FOCUS, guidance.getActionType());
    assertEquals(0L, guidance.getRecommendedContributionAmount());
    assertEquals(StrategyApplicationSourceType.INVESTMENT_GUIDANCE, first.getSourceType());
    assertEquals(first.getApplicationId(), duplicate.getApplicationId());
    assertEquals(1, applicationMapper.applications.size());
    assertEquals(RecurringInvestmentPlanStatus.SAFE_FOCUS, planMapper.plan.getStatus());
    assertEquals(0L, planMapper.plan.getContributionAmount());
  }

  @Test
  void rejectsGuidanceCreationAndLookupWhenOnboardingGoalIsInvalid() {
    InMemoryGuidanceMapper guidanceMapper = new InMemoryGuidanceMapper();
    InvestmentGuidanceService service =
        new InvestmentGuidanceServiceImpl(
            guidanceMapper,
            new StubGuidanceInputMapper(0L),
            new InMemoryPlanMapper(),
            new InMemoryApplicationMapper(),
            (userId, plan) -> {
              throw new AssertionError("목표 미설정 상태에서는 증권 API를 호출하면 안 됩니다.");
            },
            new InvestmentGuidanceCalculator(),
            new TemplateInvestmentGuidanceReasonGenerator(),
            FIXED_CLOCK);

    InvestmentGuidanceException createException =
        assertThrows(InvestmentGuidanceException.class, () -> service.create(1L));
    InvestmentGuidanceException lookupException =
        assertThrows(InvestmentGuidanceException.class, () -> service.getLatest(1L));

    assertEquals(InvestmentGuidanceErrorCode.INPUT_NOT_READY, createException.getErrorCode());
    assertEquals(InvestmentGuidanceErrorCode.INPUT_NOT_READY, lookupException.getErrorCode());
    assertEquals(0, guidanceMapper.guidances.size());
  }

  @Test
  void latestRejectsExpiredGuidance() {
    InMemoryGuidanceMapper guidanceMapper = new InMemoryGuidanceMapper();
    InvestmentGuidanceService service =
        new InvestmentGuidanceServiceImpl(
            guidanceMapper,
            new StubGuidanceInputMapper(),
            new InMemoryPlanMapper(),
            new InMemoryApplicationMapper(),
            (userId, plan) ->
                new BrokeragePositionSnapshot(
                    1_100_000L,
                    1_000_000L,
                    1_100_000L,
                    100_000L,
                    new BigDecimal("10.0000"),
                    LocalDateTime.of(2026, 8, 4, 9, 0)),
            new InvestmentGuidanceCalculator(),
            new TemplateInvestmentGuidanceReasonGenerator(),
            FIXED_CLOCK);

    service.create(1L);
    guidanceMapper.guidances.get(0).setNextReviewAt(LocalDateTime.now(FIXED_CLOCK));

    InvestmentGuidanceException exception =
        assertThrows(InvestmentGuidanceException.class, () -> service.getLatest(1L));
    assertEquals(InvestmentGuidanceErrorCode.NOT_FOUND, exception.getErrorCode());
  }

  private static class StubGuidanceInputMapper implements InvestmentGuidanceInputMapper {

    private final long targetAmount;

    private StubGuidanceInputMapper() {
      this(20_000_000L);
    }

    private StubGuidanceInputMapper(long targetAmount) {
      this.targetAmount = targetAmount;
    }

    @Override
    public Long findActiveTargetAmountByUserId(long userId) {
      return targetAmount;
    }

    @Override
    public InvestmentGuidanceInputSourceVo findLatestByUserId(
        long userId, long brokerageAccountId) {
      return InvestmentGuidanceInputSourceVo.builder()
          .forecastId(30L)
          .baselineExpectedAsset(20_300_000L)
          .brokerageBookValue(1_100_000L)
          .targetAmount(targetAmount)
          .rankName("병장")
          .dischargeDate(LocalDate.of(2027, 8, 4))
          .expectedReturnRate(new BigDecimal("5.00"))
          .build();
    }
  }

  private static class InMemoryGuidanceMapper implements InvestmentGuidanceMapper {

    private final List<InvestmentGuidanceVo> guidances = new ArrayList<>();

    @Override
    public int insert(InvestmentGuidanceVo guidance) {
      guidance.setGuidanceId((long) guidances.size() + 1);
      guidance.setCreatedAt(LocalDateTime.of(2026, 8, 4, 9, 0));
      guidances.add(guidance);
      return 1;
    }

    @Override
    public InvestmentGuidanceVo findByIdAndUserId(long guidanceId, long userId) {
      return guidances.stream()
          .filter(item -> item.getGuidanceId() == guidanceId && item.getUserId() == userId)
          .findFirst()
          .orElse(null);
    }

    @Override
    public InvestmentGuidanceVo findLatestByUserId(long userId, LocalDateTime now) {
      return guidances.stream()
          .filter(item -> item.getUserId() == userId)
          .filter(item -> item.getNextReviewAt() != null && item.getNextReviewAt().isAfter(now))
          .max(Comparator.comparing(InvestmentGuidanceVo::getGuidanceId))
          .orElse(null);
    }

    @Override
    public InvestmentGuidanceVo findLatestByUserIdAndInputDataHash(
        long userId, String inputDataHash) {
      return guidances.stream()
          .filter(
              item ->
                  item.getUserId() == userId
                      && Objects.equals(item.getInputDataHash(), inputDataHash)
                      && item.getActionType() != InvestmentGuidanceAction.REVIEW)
          .findFirst()
          .orElse(null);
    }
  }

  private static class InMemoryPlanMapper implements RecurringInvestmentPlanMapper {

    private RecurringInvestmentPlanVo plan =
        RecurringInvestmentPlanVo.builder()
            .planId(10L)
            .userId(1L)
            .brokerageAccountId(7L)
            .frequency(InvestmentFrequency.MONTHLY)
            .contributionDay(10)
            .contributionAmount(150_000L)
            .maximumMonthlyAmount(200_000L)
            .investmentProductCode("069500")
            .investmentProductName("KODEX 200")
            .status(RecurringInvestmentPlanStatus.ACTIVE)
            .build();

    @Override
    public int insert(RecurringInvestmentPlanVo plan) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int update(RecurringInvestmentPlanVo plan) {
      this.plan = plan;
      return 1;
    }

    @Override
    public RecurringInvestmentPlanVo findByUserId(long userId) {
      return plan.getUserId() == userId ? plan : null;
    }

    @Override
    public RecurringInvestmentPlanVo findByIdAndUserId(long planId, long userId) {
      return plan.getPlanId() == planId && plan.getUserId() == userId ? plan : null;
    }

    @Override
    public boolean existsSecuritiesAccount(long accountId, long userId) {
      return true;
    }
  }

  private static class InMemoryApplicationMapper implements StrategyApplicationMapper {

    private final List<StrategyApplicationVo> applications = new ArrayList<>();

    @Override
    public int insert(StrategyApplicationVo application) {
      application.setApplicationId((long) applications.size() + 1);
      application.setAppliedAt(LocalDateTime.of(2026, 8, 4, 9, 1));
      applications.add(application);
      return 1;
    }

    @Override
    public int updateAfterExpectedAsset(
        long applicationId, long userId, long afterExpectedAsset) {
      StrategyApplicationVo application = findByIdAndUserId(applicationId, userId);
      if (application == null) {
        return 0;
      }
      application.setAfterExpectedAsset(afterExpectedAsset);
      return 1;
    }

    @Override
    public StrategyApplicationVo findByIdAndUserId(long applicationId, long userId) {
      return applications.stream()
          .filter(item -> item.getApplicationId() == applicationId && item.getUserId() == userId)
          .findFirst()
          .orElse(null);
    }

    @Override
    public StrategyApplicationVo findLatestByUserId(long userId) {
      return applications.stream()
          .filter(item -> item.getUserId() == userId)
          .max(Comparator.comparing(StrategyApplicationVo::getApplicationId))
          .orElse(null);
    }

    @Override
    public StrategyApplicationVo findByGuidanceSelection(
        long userId,
        long guidanceId,
        InvestmentGuidanceAction action,
        InvestmentFrequency frequency,
        long contributionAmount) {
      return applications.stream()
          .filter(
              item ->
                  item.getUserId() == userId
                      && Objects.equals(item.getGuidanceId(), guidanceId)
                      && item.getAppliedGuidanceAction() == action
                      && item.getAppliedInvestmentFrequency() == frequency
                      && Objects.equals(item.getAppliedRecurringContributionAmount(), contributionAmount))
          .findFirst()
          .orElse(null);
    }

    @Override
    public List<StrategyApplicationVo> findByUserId(long userId, long offset, int limit) {
      return applications.stream()
          .filter(item -> item.getUserId() == userId)
          .skip(offset)
          .limit(limit)
          .toList();
    }
  }
}
