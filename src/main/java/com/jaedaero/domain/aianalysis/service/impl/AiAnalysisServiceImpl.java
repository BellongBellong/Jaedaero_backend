package com.jaedaero.domain.aianalysis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResult;
import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingExpectedEffectResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingImprovementResponse;
import com.jaedaero.domain.aianalysis.exception.AiAnalysisErrorCode;
import com.jaedaero.domain.aianalysis.exception.AiAnalysisException;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrative;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerator;
import com.jaedaero.domain.aianalysis.llm.AiGenerationTask;
import com.jaedaero.domain.aianalysis.llm.OpenAiModel;
import com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper;
import com.jaedaero.domain.aianalysis.service.AiAnalysisInput;
import com.jaedaero.domain.aianalysis.service.AiAnalysisInputProvider;
import com.jaedaero.domain.aianalysis.service.AiAnalysisService;
import com.jaedaero.domain.aianalysis.service.RecurringPaymentMetric;
import com.jaedaero.domain.aianalysis.service.SpendingAnalysis;
import com.jaedaero.domain.aianalysis.service.SpendingCategoryMetric;
import com.jaedaero.domain.aianalysis.service.SpendingPatternAnalyzer;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import com.jaedaero.domain.cashflow.service.AppliedCashflowStrategy;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.SimulationAllocationPolicy;
import com.jaedaero.domain.simulation.service.SimulationCalculationResult;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import com.jaedaero.domain.simulation.service.SimulationInputProvider;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.Locale;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Primary
@Service
@RequiredArgsConstructor
public class AiAnalysisServiceImpl implements AiAnalysisService {
  private static final String PROMPT_VERSION = "openai-chat-v5-consumption-analysis";
  private final AiAnalysisMapper analysisMapper;
  private final AiAnalysisInputProvider analysisInputProvider;
  private final SimulationMapper simulationMapper;
  private final SimulationInputProvider simulationInputProvider;
  private final SimulationCalculator simulationCalculator;
  private final ObjectMapper objectMapper;
  private final AiCoachNarrativeGenerator narrativeGenerator;
  private final SpendingPatternAnalyzer spendingPatternAnalyzer;

  @Override
  @Transactional
  public AiAnalysisResponse analyze(long userId, AiAnalysisRequest request) {
    OpenAiModel model = AiGenerationTask.AI_COACH.model();
    AiAnalysisInput baseline = analysisInputProvider.load(userId);
    SimulationVo simulation = simulation(request.getSimulationId(), userId);
    long targetAmount = targetAmount(baseline, simulation);
    SimulationInput simulationInput =
        withTargetAmount(simulationInputProvider.load(userId), targetAmount);
    LocalDate today = baseline.spendingPattern().periodEnd();
    SimulationRequest currentPlan = currentPlan(simulationInput, simulation, today);
    SimulationCalculationResult currentCalculation =
        simulationCalculator.calculate(simulationInput, currentPlan, today);
    SpendingAnalysis measuredSpending =
        spendingPatternAnalyzer.analyze(
            baseline.spendingPattern(),
            currentCalculation.expectedAsset(),
            baseline.actualDischargeDate());
    AiRecommendedScenarioVo recommendation =
        recommendation(
            userId,
            simulationInput,
            currentPlan,
            measuredSpending.improvement().getSuggestedMonthlyReductionAmount(),
            today);
    SpendingAnalysis spendingAnalysis =
        alignExpectedEffect(measuredSpending, currentPlan, currentCalculation, recommendation);
    String hash = hash(baseline, simulation, currentCalculation, recommendation, model);
    AiAnalysisVo cached = analysisMapper.findLatestSuccessfulByUserIdAndInputDataHash(userId, hash);
    if (cached != null) {
      return response(cached, read(cached.getResultJson()), AiGenerationSource.CACHE);
    }

    AiAnalysisResult result =
        result(
            baseline,
            simulation,
            currentCalculation,
            recommendation,
            spendingAnalysis);
    AiGenerationSource generationSource =
        applyNarrative(model, baseline, simulation, result, recommendation);
    analysisMapper.insertRecommendedScenario(recommendation);
    result.getRecommendedScenario().setScenarioId(recommendation.getScenarioId());
    AiAnalysisVo analysis = AiAnalysisVo.builder().userId(userId).snapshotId(baseline.snapshotId())
        .simulationId(simulation == null ? null : simulation.getSimulationId())
        .analysisType(AiAnalysisType.CONSUMPTION)
        .resultJson(write(result)).inputDataHash(hash).modelName(model.apiName())
        .promptVersion(PROMPT_VERSION).generationSource(generationSource).build();
    analysisMapper.insertAnalysis(analysis);
    return response(analysis, result, generationSource);
  }

  @Override
  @Transactional(readOnly = true)
  public AiAnalysisResponse getDetail(long userId, long analysisId) {
    AiAnalysisVo analysis = analysisMapper.findAnalysisByIdAndUserId(analysisId, userId);
    if (analysis == null) throw new AiAnalysisException(AiAnalysisErrorCode.NOT_FOUND, "AI 분석 결과를 찾을 수 없습니다.");
    return response(analysis, read(analysis.getResultJson()), analysis.getGenerationSource());
  }

  private SimulationVo simulation(Long simulationId, long userId) {
    if (simulationId == null) return null;
    SimulationVo item = simulationMapper.findByIdAndUserId(simulationId, userId);
    if (item == null) throw new AiAnalysisException(AiAnalysisErrorCode.NOT_FOUND, "시뮬레이션 이력을 찾을 수 없습니다.");
    return item;
  }

  private AiRecommendedScenarioVo recommendation(
      long userId,
      SimulationInput input,
      SimulationRequest currentPlan,
      long suggestedMonthlyReductionAmount,
      LocalDate today) {
    long spending =
        Math.max(0L, currentPlan.getMonthlySpendingAmount() - suggestedMonthlyReductionAmount);
    SimulationRequest request = new SimulationRequest();
    request.setMonthlySavingAmount(currentPlan.getMonthlySavingAmount());
    request.setMonthlySpendingAmount(spending);
    request.setMonthlyInvestmentAmount(currentPlan.getMonthlyInvestmentAmount());
    request.setExpectedReturnRate(currentPlan.getExpectedReturnRate());
    SimulationCalculationResult calculated = simulationCalculator.calculate(input, request, today);
    return AiRecommendedScenarioVo.builder()
        .userId(userId)
        .monthlySavingAmount(currentPlan.getMonthlySavingAmount())
        .monthlySpendingAmount(spending)
        .monthlyInvestmentAmount(currentPlan.getMonthlyInvestmentAmount())
        .expectedReturnRate(currentPlan.getExpectedReturnRate())
        .expectedAsset(calculated.expectedAsset())
        .financialDischargeDate(calculated.financialDischargeDate())
        .recommendReason(reason(currentPlan.getMonthlySpendingAmount(), spending))
        .build();
  }

  private AiAnalysisResult result(
      AiAnalysisInput base,
      SimulationVo simulation,
      SimulationCalculationResult currentCalculation,
      AiRecommendedScenarioVo recommended,
      SpendingAnalysis spendingAnalysis) {
    long expected = currentCalculation.expectedAsset();
    long targetAmount = targetAmount(base, simulation);
    LocalDate financial = currentCalculation.financialDischargeDate();
    return AiAnalysisResult.builder().expectedAsset(expected).financialDischargeDate(financial).deltaDaysVsActual(days(base.actualDischargeDate(), financial))
        .achievementRate(rate(expected, targetAmount)).comment(comment(expected, targetAmount))
        .recommendedScenario(AiRecommendedScenarioResponse.from(recommended))
        .baselineDischargeDate(null)
        .deltaDaysVsBaseline(null)
        .pros(null)
        .cons(null)
        .spendingPattern(spendingAnalysis.pattern())
        .spendingInsights(spendingAnalysis.insights())
        .spendingImprovement(spendingAnalysis.improvement())
        .spendingExpectedEffect(spendingAnalysis.expectedEffect())
        .build();
  }

  private AiAnalysisResponse response(
      AiAnalysisVo a, AiAnalysisResult r, AiGenerationSource generationSource) {
    return AiAnalysisResponse.builder().analysisId(a.getAnalysisId()).simulationId(a.getSimulationId()).snapshotId(a.getSnapshotId())
        .analysisType(a.getAnalysisType()).generationSource(generationSource).expectedAsset(r.getExpectedAsset()).financialDischargeDate(r.getFinancialDischargeDate()).deltaDaysVsActual(r.getDeltaDaysVsActual())
        .achievementRate(r.getAchievementRate()).comment(r.getComment()).recommendedScenario(r.getRecommendedScenario())
        .baselineDischargeDate(r.getBaselineDischargeDate()).deltaDaysVsBaseline(r.getDeltaDaysVsBaseline()).pros(r.getPros()).cons(r.getCons())
        .spendingPattern(r.getSpendingPattern()).spendingInsights(r.getSpendingInsights())
        .spendingImprovement(r.getSpendingImprovement()).spendingExpectedEffect(r.getSpendingExpectedEffect()).build();
  }

  private String comment(long expected, long targetAmount) {
    long shortfall = Math.max(0, targetAmount - expected);
    return shortfall == 0
        ? "현재 What-if 계획이라면 목표 자산 달성이 예상됩니다. 실제 소비 패턴에서 줄일 수 있는 항목을 확인해보세요."
        : "현재 What-if 계획의 전역 예상 자산은 "
            + money(expected)
            + "원이며, 소비 조정 전 목표까지 "
            + money(shortfall)
            + "원이 더 필요합니다.";
  }
  private String reason(long currentSpending, long recommendedSpending) {
    long reduction = currentSpending - recommendedSpending;
    return reduction == 0
        ? "현재 What-if 소비 계획을 유지하고 반복 결제 항목을 정기적으로 확인해보세요."
        : "현재 What-if의 적금·투자 조건은 유지하고 월 소비를 "
            + money(reduction)
            + "원 줄인 "
            + money(recommendedSpending)
            + "원 이하로 조정하는 추천입니다.";
  }
  private AiGenerationSource applyNarrative(OpenAiModel model, AiAnalysisInput base, SimulationVo simulation, AiAnalysisResult result, AiRecommendedScenarioVo recommended) {
    try {
      AiCoachNarrative narrative = narrativeGenerator.generate(model, prompt(base, simulation, result, recommended));
      result.setComment(narrative.comment());
      recommended.setRecommendReason(narrative.recommendReason());
      result.getRecommendedScenario().setRecommendReason(narrative.recommendReason());
      return AiGenerationSource.OPENAI;
    } catch (AiCoachNarrativeGenerationException e) {
      log.warn("AI 코치 문구 생성에 실패해 템플릿 문구로 대체합니다. model={}, reason={}", model.apiName(), e.getMessage());
      return AiGenerationSource.FALLBACK;
    }
  }
  private String prompt(AiAnalysisInput base, SimulationVo simulation, AiAnalysisResult result, AiRecommendedScenarioVo recommended) {
    return "[분석 기준] 현재 What-if 계획과 실제 소비 패턴\n"
        + "목표 자산: " + money(targetAmount(base, simulation)) + "원\n"
        + "현재 What-if 예상 전역 자산: " + money(result.getExpectedAsset()) + "원\n"
        + spendingPrompt(result)
        + "목표 달성률: " + result.getAchievementRate() + "%\n"
        + "재정적 전역일: " + result.getFinancialDischargeDate() + "\n"
        + "실제 전역일 대비 차이: " + result.getDeltaDaysVsActual() + "일\n"
        + "유지할 장병내일준비적금 월 납입액: " + money(recommended.getMonthlySavingAmount()) + "원\n"
        + "추천 월 소비 한도: " + money(recommended.getMonthlySpendingAmount()) + "원\n"
        + "유지할 월 투자금액: " + money(recommended.getMonthlyInvestmentAmount()) + "원\n"
        + "유지할 기대수익률: " + recommended.getExpectedReturnRate() + "%\n"
        + "위 확정값을 변경하거나 새 숫자를 만들지 말고 comment에는 소비 변화의 원인과 개선 방향을 포함한 결과 해석, "
        + "recommendReason에는 실행 시 유의점을 작성하세요.";
  }
  private String hash(
      AiAnalysisInput b,
      SimulationVo s,
      SimulationCalculationResult currentCalculation,
      AiRecommendedScenarioVo recommendation,
      OpenAiModel model) {
    String raw =
        "analysis-v5-consumption-analysis|"
            + PROMPT_VERSION
            + "|"
            + model.apiName()
            + "|"
            + b.forecastId()
            + "|"
            + b.snapshotId()
            + "|"
            + targetAmount(b, s)
            + "|"
            + b.expectedAsset()
            + "|current-plan|"
            + currentCalculation.expectedAsset()
            + "|"
            + currentCalculation.financialDischargeDate()
            + "|"
            + (s == null
                ? "baseline"
                : s.getSimulationId()
                    + "|"
                    + s.getTargetAmount()
                    + "|"
                    + s.getMonthlySavingAmount()
                    + "|"
                    + s.getMonthlyInvestmentAmount()
                    + "|"
                    + s.getExpectedReturnRate()
                    + "|"
                    + s.getMonthlySpendingAmount()
                    + "|"
                    + s.getExpectedAsset())
            + "|recommendation|"
            + recommendation.getMonthlySavingAmount()
            + "|"
            + recommendation.getMonthlyInvestmentAmount()
            + "|"
            + recommendation.getExpectedReturnRate()
            + "|"
            + recommendation.getMonthlySpendingAmount()
            + "|"
            + recommendation.getExpectedAsset()
            + "|spending-pattern|"
            + spendingHashMaterial(b);
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception e) { throw new IllegalStateException("AI 분석 입력 해시를 생성할 수 없습니다.", e); }
  }
  private AiAnalysisResult read(String json) { try { return objectMapper.readValue(json, AiAnalysisResult.class); } catch (JsonProcessingException e) { throw new AiAnalysisException(AiAnalysisErrorCode.RESULT_UNREADABLE, "저장된 AI 분석 결과를 읽을 수 없습니다.", e); } }
  private String write(AiAnalysisResult result) { try { return objectMapper.writeValueAsString(result); } catch (JsonProcessingException e) { throw new AiAnalysisException(AiAnalysisErrorCode.RESULT_UNREADABLE, "AI 분석 결과를 저장할 수 없습니다.", e); } }
  private SimulationRequest currentPlan(
      SimulationInput input, SimulationVo simulation, LocalDate today) {
    SimulationRequest request = new SimulationRequest();
    if (simulation != null) {
      request.setMonthlySpendingAmount(simulation.getMonthlySpendingAmount());
      request.setMonthlySavingAmount(simulation.getMonthlySavingAmount());
      request.setMonthlyInvestmentAmount(simulation.getMonthlyInvestmentAmount());
      request.setExpectedReturnRate(simulation.getExpectedReturnRate());
      return request;
    }

    AppliedCashflowStrategy applied = input.appliedStrategy();
    if (applied != null) {
      request.setMonthlySpendingAmount(applied.monthlySpendingAmount());
      request.setMonthlySavingAmount(applied.monthlySavingAmount());
      request.setMonthlyInvestmentAmount(applied.monthlyInvestmentAmount());
      request.setExpectedReturnRate(applied.expectedReturnRate());
      return request;
    }

    long referenceMonthlyIncome = simulationCalculator.referenceMonthlyIncome(input, today);
    request.setMonthlySpendingAmount(0L);
    request.setMonthlySavingAmount(
        Math.min(SimulationAllocationPolicy.MAX_MONTHLY_SAVING_AMOUNT, referenceMonthlyIncome));
    request.setMonthlyInvestmentAmount(0L);
    request.setExpectedReturnRate(SimulationAllocationPolicy.DEFAULT_EXPECTED_RETURN_RATE);
    return request;
  }

  private SpendingAnalysis alignExpectedEffect(
      SpendingAnalysis measured,
      SimulationRequest currentPlan,
      SimulationCalculationResult currentCalculation,
      AiRecommendedScenarioVo recommendation) {
    long currentSpending = currentPlan.getMonthlySpendingAmount();
    long recommendedSpending = recommendation.getMonthlySpendingAmount();
    long appliedReduction = Math.max(0L, currentSpending - recommendedSpending);
    long expectedIncrease =
        Math.max(0L, recommendation.getExpectedAsset() - currentCalculation.expectedAsset());
    SpendingImprovementResponse improvement =
        SpendingImprovementResponse.builder()
            .targetCategory(measured.improvement().getTargetCategory())
            .suggestedMonthlyReductionAmount(appliedReduction)
            .action(
                appliedReduction == 0
                    ? "현재 What-if 소비 계획을 유지하고 반복 결제 항목을 정기적으로 확인하세요."
                    : "현재 What-if 월 소비를 "
                        + money(appliedReduction)
                        + "원 줄여 "
                        + money(recommendedSpending)
                        + "원으로 조정해보세요.")
            .build();
    SpendingExpectedEffectResponse expectedEffect =
        SpendingExpectedEffectResponse.builder()
            .remainingMonths(measured.expectedEffect().getRemainingMonths())
            .expectedAssetIncreaseAmount(expectedIncrease)
            .expectedAssetAfterImprovement(recommendation.getExpectedAsset())
            .assumption(
                "현재 적용 중인 What-if의 적금·투자·수익률은 유지하고 월 소비만 추천 한도로 조정한 계산값입니다.")
            .build();
    return new SpendingAnalysis(
        measured.pattern(), measured.insights(), improvement, expectedEffect);
  }

  private SimulationInput withTargetAmount(SimulationInput input, long targetAmount) {
    return new SimulationInput(
        input.userId(),
        input.baseAsset(),
        targetAmount,
        input.monthlySpendingAverage(),
        input.soldierType(),
        input.enlistmentDate(),
        input.dischargeDate(),
        input.soldierSavings(),
        input.appliedStrategy());
  }
  private long targetAmount(AiAnalysisInput base, SimulationVo simulation) { return simulation == null ? base.targetAmount() : simulation.getTargetAmount(); }
  private String spendingPrompt(AiAnalysisResult result) {
    StringBuilder prompt = new StringBuilder();
    prompt.append("소비 비교 기간: ")
        .append(result.getSpendingPattern().getPeriodStart()).append("~").append(result.getSpendingPattern().getPeriodEnd())
        .append(" vs ").append(result.getSpendingPattern().getComparisonPeriodStart()).append("~")
        .append(result.getSpendingPattern().getComparisonPeriodEnd()).append("\n")
        .append("현재 소비 합계: ").append(money(result.getSpendingPattern().getTotalSpendingAmount())).append("원\n")
        .append("전월 동일 기간 소비 합계: ")
        .append(money(result.getSpendingPattern().getPreviousTotalSpendingAmount())).append("원\n");
    result.getSpendingPattern().getCategories().forEach(category ->
        prompt.append("카테고리 ").append(category.getDisplayName()).append(": ")
            .append(money(category.getAmount())).append("원, 전월 ")
            .append(money(category.getPreviousAmount())).append("원, 거래 ")
            .append(category.getTransactionCount()).append("건\n"));
    result.getSpendingInsights().forEach(insight ->
        prompt.append("소비 분석 근거 ").append(insight.getTitle()).append(": ")
            .append(insight.getDescription()).append(" (현재 ")
            .append(money(insight.getEvidence().getCurrentAmount())).append("원, 전월 ")
            .append(money(insight.getEvidence().getPreviousAmount())).append("원)\n"));
    prompt.append("제안 월 소비 절감액: ")
        .append(money(result.getSpendingImprovement().getSuggestedMonthlyReductionAmount())).append("원\n")
        .append("절감 유지 시 예상 자산 증가액: ")
        .append(money(result.getSpendingExpectedEffect().getExpectedAssetIncreaseAmount())).append("원\n");
    return prompt.toString();
  }
  private String spendingHashMaterial(AiAnalysisInput input) {
    StringBuilder material = new StringBuilder()
        .append(input.spendingPattern().periodStart()).append('|')
        .append(input.spendingPattern().periodEnd()).append('|')
        .append(input.spendingPattern().comparisonPeriodStart()).append('|')
        .append(input.spendingPattern().comparisonPeriodEnd());
    for (SpendingCategoryMetric category : input.spendingPattern().categories()) {
      material.append('|').append(category.category()).append(':')
          .append(category.currentAmount()).append(':').append(category.previousAmount()).append(':')
          .append(category.currentTransactionCount()).append(':').append(category.previousTransactionCount());
    }
    for (RecurringPaymentMetric payment : input.spendingPattern().recurringPayments()) {
      material.append("|recurring:").append(payment.description()).append(':')
          .append(payment.currentAmount()).append(':').append(payment.previousAmount()).append(':')
          .append(payment.currentTransactionCount()).append(':').append(payment.previousTransactionCount());
    }
    return material.toString();
  }
  private BigDecimal rate(long expected, long target) { return target == 0 ? new BigDecimal("100.00") : BigDecimal.valueOf(expected).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP); }
  private Integer days(LocalDate start, LocalDate end) { return start == null || end == null ? null : Math.toIntExact(ChronoUnit.DAYS.between(start, end)); }
  private String money(long value) { return String.format(Locale.KOREA, "%,d", value); }
}
