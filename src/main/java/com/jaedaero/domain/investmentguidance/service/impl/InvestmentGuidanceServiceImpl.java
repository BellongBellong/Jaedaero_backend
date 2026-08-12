package com.jaedaero.domain.investmentguidance.service.impl;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceDetailResponse;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceErrorCode;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceException;
import com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceInputMapper;
import com.jaedaero.domain.investmentguidance.mapper.InvestmentGuidanceMapper;
import com.jaedaero.domain.investmentguidance.service.BrokeragePositionProvider;
import com.jaedaero.domain.investmentguidance.service.BrokeragePositionSnapshot;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceCalculationInput;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceCalculationResult;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceCalculator;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceReasonGenerator;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceService;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceInputSourceVo;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceVo;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.service.RecurringInvestmentSchedule;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanStatus;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.HexFormat;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class InvestmentGuidanceServiceImpl implements InvestmentGuidanceService {

  private final InvestmentGuidanceMapper guidanceMapper;
  private final InvestmentGuidanceInputMapper inputMapper;
  private final RecurringInvestmentPlanMapper planMapper;
  private final StrategyApplicationMapper applicationMapper;
  private final BrokeragePositionProvider brokeragePositionProvider;
  private final InvestmentGuidanceCalculator calculator;
  private final InvestmentGuidanceReasonGenerator reasonGenerator;
  private final Clock clock;

  public InvestmentGuidanceServiceImpl(
      InvestmentGuidanceMapper guidanceMapper,
      InvestmentGuidanceInputMapper inputMapper,
      RecurringInvestmentPlanMapper planMapper,
      StrategyApplicationMapper applicationMapper,
      BrokeragePositionProvider brokeragePositionProvider,
      InvestmentGuidanceCalculator calculator,
      InvestmentGuidanceReasonGenerator reasonGenerator,
      Clock clock) {
    this.guidanceMapper = guidanceMapper;
    this.inputMapper = inputMapper;
    this.planMapper = planMapper;
    this.applicationMapper = applicationMapper;
    this.brokeragePositionProvider = brokeragePositionProvider;
    this.calculator = calculator;
    this.reasonGenerator = reasonGenerator;
    this.clock = clock;
  }

  @Override
  @Transactional(readOnly = true)
  public InvestmentGuidanceResponse getLatest(long userId) {
    requiredConfiguredTargetAmount(userId);
    InvestmentGuidanceVo guidance =
        guidanceMapper.findLatestByUserId(userId, LocalDateTime.now(clock));
    if (guidance == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.NOT_FOUND, "생성된 적립식 투자 가이드가 없습니다.");
    }
    return InvestmentGuidanceResponse.from(guidance);
  }

  @Override
  @Transactional(readOnly = true)
  public InvestmentGuidanceDetailResponse getDetail(long userId, long guidanceId) {
    InvestmentGuidanceVo guidance = guidanceMapper.findByIdAndUserId(guidanceId, userId);
    if (guidance == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.NOT_FOUND, "조회할 적립식 투자 가이드를 찾을 수 없습니다.");
    }
    RecurringInvestmentPlanVo plan = planMapper.findByIdAndUserId(guidance.getPlanId(), userId);
    if (plan == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY, "가이드에 연결된 적립 계획을 찾을 수 없습니다.");
    }
    return InvestmentGuidanceDetailResponse.from(
        guidance,
        plan,
        inputMapper.findCurrentRankNameByUserId(userId),
        applicationMapper.findLatestByGuidanceIdAndUserId(guidanceId, userId));
  }

  @Override
  @Transactional
  public InvestmentGuidanceResponse create(long userId) {
    long targetAmount = requiredConfiguredTargetAmount(userId);
    RecurringInvestmentPlanVo plan = requiredPlan(userId);
    InvestmentGuidanceInputSourceVo source =
        inputMapper.findLatestByUserId(userId, plan.getBrokerageAccountId());
    if (source == null
        || source.getForecastId() == null
        || source.getBaselineExpectedAsset() == null
        || source.getTargetAmount() == null
        || source.getDischargeDate() == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "가이드 계산에 필요한 최신 캐시플로우, 목표 또는 군 복무 정보가 없습니다.");
    }
    if (source.getTargetAmount() != targetAmount) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "목표 변경 후 캐시플로우 계산이 완료되지 않았습니다. 잠시 후 다시 시도해 주세요.");
    }

    LocalDate today = LocalDate.now(clock);
    LocalDateTime now = LocalDateTime.now(clock);
    BrokeragePositionSnapshot position;
    String dataIssue = null;
    try {
      position = brokeragePositionProvider.load(userId, plan);
    } catch (RuntimeException exception) {
      position =
          new BrokeragePositionSnapshot(
              0, 0, 0, 0, 0, 0, BigDecimal.ZERO.setScale(4), null);
      dataIssue = "증권 데이터 조회 지연";
    }

    long baselineExpectedAsset = source.getBaselineExpectedAsset();
    if (dataIssue == null) {
      baselineExpectedAsset =
          Math.addExact(
              Math.subtractExact(
                  baselineExpectedAsset,
                  source.getBrokerageBookValue() == null ? 0L : source.getBrokerageBookValue()),
              position.accountValue());
    }
    InvestmentGuidanceCalculationInput calculationInput =
        new InvestmentGuidanceCalculationInput(
            baselineExpectedAsset,
            source.getTargetAmount(),
            source.getRankName(),
            source.getDischargeDate(),
            source.getExpectedReturnRate() == null ? BigDecimal.ZERO : source.getExpectedReturnRate(),
            plan);
    InvestmentGuidanceCalculationResult calculation = calculator.calculate(calculationInput, today);

    InvestmentGuidanceAction action =
        dataIssue == null ? calculation.actionType() : InvestmentGuidanceAction.REVIEW;
    long recommendedAmount =
        action == InvestmentGuidanceAction.REVIEW
            ? calculation.currentContributionAmount()
            : calculation.recommendedContributionAmount();
    long recommendedExpectedAsset =
        action == InvestmentGuidanceAction.REVIEW
            ? calculation.continueExpectedAsset()
            : calculation.recommendedExpectedAsset();
    LocalDate nextReviewDate =
        RecurringInvestmentSchedule.nextDate(
            today, plan.getFrequency(), plan.getContributionDay());
    LocalDateTime nextReviewAt = LocalDateTime.of(nextReviewDate, LocalTime.MIDNIGHT);
    String inputDataHash = hash(plan, source, position);

    if (action != InvestmentGuidanceAction.REVIEW) {
      InvestmentGuidanceVo cached =
          guidanceMapper.findLatestByUserIdAndInputDataHash(userId, inputDataHash);
      if (cached != null
          && cached.getNextReviewAt() != null
          && cached.getNextReviewAt().isAfter(now)) {
        return InvestmentGuidanceResponse.from(cached);
      }
    }

    InvestmentGuidanceVo guidance =
        InvestmentGuidanceVo.builder()
            .userId(userId)
            .planId(plan.getPlanId())
            .forecastId(source.getForecastId())
            .planUpdatedAt(plan.getUpdatedAt())
            .inputDataHash(inputDataHash)
            .serviceStage(calculation.serviceStage())
            .actionType(action)
            .targetAmount(source.getTargetAmount())
            .currentContributionAmount(calculation.currentContributionAmount())
            .recommendedContributionAmount(recommendedAmount)
            .continueExpectedAsset(calculation.continueExpectedAsset())
            .recommendedExpectedAsset(recommendedExpectedAsset)
            .investmentPrincipal(position.investmentPrincipal())
            .marketValue(position.marketValue())
            .unrealizedProfitLoss(position.unrealizedProfitLoss())
            .returnRate(position.returnRate())
            .safeAssetAmount(position.safeAssetAmount())
            .riskAssetAmount(position.riskAssetAmount())
            .expectedReturnRate(source.getExpectedReturnRate() == null ? BigDecimal.ZERO : source.getExpectedReturnRate())
            .remainingContributionCount(calculation.remainingContributionCount())
            .safetyBufferAmount(calculation.safetyBufferAmount())
            .reason(
                reasonGenerator.generate(
                    action,
                    calculation.currentContributionAmount(),
                    recommendedAmount,
                    recommendedExpectedAsset,
                    source.getTargetAmount(),
                    dataIssue))
            .marketDataAsOf(position.marketDataAsOf())
            .nextReviewAt(nextReviewAt)
            .build();
    guidanceMapper.insert(guidance);
    InvestmentGuidanceVo saved = guidanceMapper.findByIdAndUserId(guidance.getGuidanceId(), userId);
    return InvestmentGuidanceResponse.from(saved == null ? guidance : saved);
  }

  @Override
  @Transactional
  public StrategyApplicationResponse apply(
      long userId, long guidanceId, InvestmentGuidanceApplyRequest request) {
    InvestmentGuidanceVo guidance = guidanceMapper.findByIdAndUserId(guidanceId, userId);
    if (guidance == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.NOT_FOUND, "적용할 투자 가이드를 찾을 수 없습니다.");
    }
    RecurringInvestmentPlanVo plan = planMapper.findByIdAndUserId(guidance.getPlanId(), userId);
    if (plan == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY, "가이드에 연결된 적립 계획을 찾을 수 없습니다.");
    }
    StrategyApplicationVo existing =
        applicationMapper.findByGuidanceSelection(
            userId,
            guidanceId,
            request.getSelectedAction(),
            plan.getFrequency(),
            request.getSelectedContributionAmount());
    if (existing != null) {
      return StrategyApplicationResponse.from(existing);
    }
    validateApplication(plan, guidance, request);

    plan.setContributionAmount(request.getSelectedContributionAmount());
    plan.setStatus(statusFor(request));
    plan.setNextContributionDate(
        RecurringInvestmentSchedule.nextDate(
            LocalDate.now(clock), plan.getFrequency(), plan.getContributionDay()));
    planMapper.update(plan);

    StrategyApplicationVo application =
        StrategyApplicationVo.builder()
            .userId(userId)
            .sourceType(StrategyApplicationSourceType.INVESTMENT_GUIDANCE)
            .guidanceId(guidanceId)
            .appliedGuidanceAction(request.getSelectedAction())
            .appliedInvestmentFrequency(plan.getFrequency())
            .appliedRecurringContributionAmount(request.getSelectedContributionAmount())
            .beforeExpectedAsset(guidance.getContinueExpectedAsset())
            .afterExpectedAsset(estimateSelectedExpectedAsset(guidance, request.getSelectedContributionAmount()))
            .build();
    applicationMapper.insert(application);
    StrategyApplicationVo saved =
        applicationMapper.findByIdAndUserId(application.getApplicationId(), userId);
    return StrategyApplicationResponse.from(saved == null ? application : saved);
  }

  private RecurringInvestmentPlanVo requiredPlan(long userId) {
    RecurringInvestmentPlanVo plan = planMapper.findByUserId(userId);
    if (plan == null) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY, "먼저 적립식 투자 계획을 설정해 주세요.");
    }
    return plan;
  }

  private long requiredConfiguredTargetAmount(long userId) {
    Long targetAmount = inputMapper.findActiveTargetAmountByUserId(userId);
    if (targetAmount == null || targetAmount <= 0) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "온보딩 완료에 필요한 활성 전역 자산 목표가 없거나 유효하지 않습니다.");
    }
    return targetAmount;
  }

  private void validateApplication(
      RecurringInvestmentPlanVo plan,
      InvestmentGuidanceVo guidance,
      InvestmentGuidanceApplyRequest request) {
    InvestmentGuidanceAction action = request.getSelectedAction();
    long amount = request.getSelectedContributionAmount();
    if (guidance.getActionType() == InvestmentGuidanceAction.REVIEW
        || action == InvestmentGuidanceAction.REVIEW) {
      throw invalidApplication("REVIEW 상태의 가이드는 적용할 수 없습니다.");
    }
    LocalDateTime now = LocalDateTime.now(clock);
    if (guidance.getNextReviewAt() == null || !guidance.getNextReviewAt().isAfter(now)) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "투자 가이드가 만료되었습니다. 최신 가이드를 다시 계산해 주세요.");
    }
    long currentTargetAmount = requiredConfiguredTargetAmount(plan.getUserId());
    if (guidance.getTargetAmount() != currentTargetAmount) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "전역 목표가 가이드 생성 후 변경되었습니다. 최신 가이드를 다시 계산해 주세요.");
    }
    if (guidance.getPlanUpdatedAt() != null
        && plan.getUpdatedAt() != null
        && !guidance.getPlanUpdatedAt().equals(plan.getUpdatedAt())) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "적립 계획이 가이드 생성 후 변경되었습니다. 최신 가이드를 다시 계산해 주세요.");
    }
    InvestmentGuidanceInputSourceVo currentSource =
        inputMapper.findLatestByUserId(plan.getUserId(), plan.getBrokerageAccountId());
    if (currentSource == null
        || currentSource.getForecastId() == null
        || !currentSource.getForecastId().equals(guidance.getForecastId())) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "캐시플로우가 가이드 생성 후 변경되었습니다. 최신 가이드를 다시 계산해 주세요.");
    }
    if (currentSource.getBrokerageLastSyncedAt() != null
        && (guidance.getMarketDataAsOf() == null
            || currentSource.getBrokerageLastSyncedAt().isAfter(guidance.getMarketDataAsOf()))) {
      throw new InvestmentGuidanceException(
          InvestmentGuidanceErrorCode.INPUT_NOT_READY,
          "증권 데이터가 가이드 생성 후 변경되었습니다. 최신 가이드를 다시 계산해 주세요.");
    }
    long monthlyEquivalent =
        RecurringInvestmentSchedule.monthlyEquivalent(plan.getFrequency(), amount);
    if (monthlyEquivalent > plan.getMaximumMonthlyAmount()) {
      throw invalidApplication("선택한 적립금의 월 환산액이 월 최대 투자한도를 초과합니다.");
    }
    long current = guidance.getCurrentContributionAmount();
    boolean valid =
        switch (action) {
          case START -> current == 0 && amount > 0;
          case CONTINUE -> amount == current;
          case REDUCE -> amount > 0 && amount < current;
          case PAUSE, SAFE_FOCUS -> amount == 0;
          case REVIEW -> false;
        };
    if (!valid) {
      throw invalidApplication("선택한 행동과 적립금이 일치하지 않습니다.");
    }
  }

  private InvestmentGuidanceException invalidApplication(String message) {
    return new InvestmentGuidanceException(
        InvestmentGuidanceErrorCode.INVALID_APPLICATION, message);
  }

  private RecurringInvestmentPlanStatus statusFor(InvestmentGuidanceApplyRequest request) {
    if (request.getSelectedAction() == InvestmentGuidanceAction.SAFE_FOCUS) {
      return RecurringInvestmentPlanStatus.SAFE_FOCUS;
    }
    if (request.getSelectedContributionAmount() == 0) {
      return RecurringInvestmentPlanStatus.PAUSED;
    }
    return RecurringInvestmentPlanStatus.ACTIVE;
  }

  private long estimateSelectedExpectedAsset(InvestmentGuidanceVo guidance, long selectedAmount) {
    long current = guidance.getCurrentContributionAmount();
    long recommended = guidance.getRecommendedContributionAmount();
    if (current == recommended) {
      return guidance.getContinueExpectedAsset();
    }
    BigDecimal expectedDelta =
        BigDecimal.valueOf(guidance.getContinueExpectedAsset() - guidance.getRecommendedExpectedAsset());
    BigDecimal amountDelta = BigDecimal.valueOf(current - recommended);
    BigDecimal selectedDelta = BigDecimal.valueOf(selectedAmount - recommended);
    return BigDecimal.valueOf(guidance.getRecommendedExpectedAsset())
        .add(expectedDelta.multiply(selectedDelta).divide(amountDelta, 0, RoundingMode.HALF_UP))
        .longValueExact();
  }

  private String hash(
      RecurringInvestmentPlanVo plan,
      InvestmentGuidanceInputSourceVo source,
      BrokeragePositionSnapshot position) {
    String canonical =
        String.join(
            "|",
            String.valueOf(plan.getPlanId()),
            String.valueOf(plan.getBrokerageAccountId()),
            plan.getFrequency().name(),
            String.valueOf(plan.getContributionDay()),
            String.valueOf(plan.getContributionAmount()),
            String.valueOf(plan.getMaximumMonthlyAmount()),
            String.valueOf(plan.getUpdatedAt()),
            plan.getInvestmentProductCode(),
            String.valueOf(source.getForecastId()),
            String.valueOf(source.getBaselineExpectedAsset()),
            String.valueOf(source.getBrokerageLastSyncedAt()),
            String.valueOf(source.getTargetAmount()),
            String.valueOf(source.getRankName()),
            String.valueOf(source.getDischargeDate()),
            String.valueOf(source.getExpectedReturnRate()),
            String.valueOf(position.investmentPrincipal()),
            String.valueOf(position.accountValue()),
            String.valueOf(position.safeAssetAmount()),
            String.valueOf(position.riskAssetAmount()),
            String.valueOf(position.marketValue()),
            String.valueOf(position.unrealizedProfitLoss()),
            String.valueOf(position.returnRate()));
    try {
      return HexFormat.of()
          .formatHex(MessageDigest.getInstance("SHA-256").digest(canonical.getBytes(StandardCharsets.UTF_8)));
    } catch (NoSuchAlgorithmException exception) {
      throw new IllegalStateException("SHA-256 해시를 사용할 수 없습니다.", exception);
    }
  }
}
