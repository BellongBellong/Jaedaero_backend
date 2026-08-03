package com.jaedaero.domain.aianalysis.service.impl;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResult;
import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
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
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
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
import java.time.YearMonth;
import java.time.temporal.ChronoUnit;
import java.util.HexFormat;
import java.util.List;
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
  private static final BigDecimal DEFAULT_RATIO = new BigDecimal("20.00");
  private static final BigDecimal DEFAULT_RETURN = new BigDecimal("5.00");
  private static final String PROMPT_VERSION = "openai-chat-v2";
  private final AiAnalysisMapper analysisMapper;
  private final AiAnalysisInputProvider analysisInputProvider;
  private final SimulationMapper simulationMapper;
  private final SimulationInputProvider simulationInputProvider;
  private final SimulationCalculator simulationCalculator;
  private final ObjectMapper objectMapper;
  private final AiCoachNarrativeGenerator narrativeGenerator;

  @Override
  @Transactional
  public AiAnalysisResponse analyze(long userId, AiAnalysisRequest request) {
    OpenAiModel model = AiGenerationTask.AI_COACH.model();
    AiAnalysisInput baseline = analysisInputProvider.load(userId);
    SimulationVo simulation = simulation(request.getSimulationId(), userId);
    String hash = hash(baseline, simulation, model);
    AiAnalysisVo cached = analysisMapper.findLatestSuccessfulByUserIdAndInputDataHash(userId, hash);
    if (cached != null) {
      return response(cached, read(cached.getResultJson()), AiGenerationSource.CACHE);
    }

    AiRecommendedScenarioVo recommendation = recommendation(userId, baseline, simulation, LocalDate.now());
    AiAnalysisResult result = result(baseline, simulation, recommendation);
    AiGenerationSource generationSource =
        applyNarrative(model, baseline, simulation, result, recommendation);
    analysisMapper.insertRecommendedScenario(recommendation);
    result.getRecommendedScenario().setScenarioId(recommendation.getScenarioId());
    AiAnalysisVo analysis = AiAnalysisVo.builder().userId(userId).snapshotId(baseline.snapshotId())
        .simulationId(simulation == null ? null : simulation.getSimulationId())
        .analysisType(simulation == null ? AiAnalysisType.DIAGNOSIS : AiAnalysisType.SCENARIO_COMPARISON)
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

  private AiRecommendedScenarioVo recommendation(long userId, AiAnalysisInput base, SimulationVo simulation, LocalDate today) {
    long current = simulation == null ? base.expectedAsset() : simulation.getExpectedAsset();
    long months = remainingMonths(today, base.actualDischargeDate());
    long extra = months == 0 ? 0 : ceil(Math.max(0, base.targetAmount() - current), months);
    long saving = simulation == null ? extra : simulation.getMonthlySavingAmount() + extra;
    long spending = simulation == null ? base.monthlySpendingLimit() : Math.max(0, simulation.getMonthlySpendingAmount() - Math.min(30_000, simulation.getMonthlySpendingAmount() / 10));
    BigDecimal ratio = simulation == null ? DEFAULT_RATIO : simulation.getInvestmentRatio();
    BigDecimal expectedReturn = simulation == null ? DEFAULT_RETURN : simulation.getExpectedReturnRate();
    SimulationRequest request = new SimulationRequest();
    request.setMonthlySavingAmount(saving); request.setMonthlySpendingAmount(spending);
    request.setInvestmentRatio(ratio); request.setExpectedReturnRate(expectedReturn);
    SimulationInput input = simulationInputProvider.load(userId);
    SimulationCalculationResult calculated = simulationCalculator.calculate(input, request, today);
    return AiRecommendedScenarioVo.builder().userId(userId).monthlySavingAmount(saving).monthlySpendingAmount(spending)
        .investmentRatio(ratio).expectedReturnRate(expectedReturn).expectedAsset(calculated.expectedAsset())
        .financialDischargeDate(calculated.financialDischargeDate()).recommendReason(reason(base, saving, spending)).build();
  }

  private AiAnalysisResult result(AiAnalysisInput base, SimulationVo simulation, AiRecommendedScenarioVo recommended) {
    long expected = simulation == null ? base.expectedAsset() : simulation.getExpectedAsset();
    LocalDate financial = simulation == null ? base.financialDischargeDate() : simulation.getFinancialDischargeDate();
    boolean scenario = simulation != null;
    long difference = expected - base.expectedAsset();
    return AiAnalysisResult.builder().financialDischargeDate(financial).deltaDaysVsActual(days(base.actualDischargeDate(), financial))
        .achievementRate(rate(expected, base.targetAmount())).comment(comment(base, expected, scenario))
        .recommendedScenario(AiRecommendedScenarioResponse.from(recommended))
        .baselineDischargeDate(scenario ? base.financialDischargeDate() : null)
        .deltaDaysVsBaseline(scenario ? days(base.financialDischargeDate(), financial) : null)
        .pros(scenario ? List.of(difference >= 0 ? "평소 예상 자산보다 " + money(difference) + "원을 더 확보할 수 있습니다." : "투자·저축 조건을 직접 조정해 결과를 비교할 수 있습니다.") : null)
        .cons(scenario ? List.of(difference < 0 ? "평소 예상 자산보다 " + money(Math.abs(difference)) + "원 줄어드는 시나리오입니다." : expected < base.targetAmount() ? "목표 자산까지 추가 저축 또는 소비 조정이 필요합니다." : "기대 수익률은 확정 수익이 아닌 가정값입니다.") : null).build();
  }

  private AiAnalysisResponse response(
      AiAnalysisVo a, AiAnalysisResult r, AiGenerationSource generationSource) {
    return AiAnalysisResponse.builder().analysisId(a.getAnalysisId()).simulationId(a.getSimulationId()).snapshotId(a.getSnapshotId())
        .analysisType(a.getAnalysisType()).generationSource(generationSource).financialDischargeDate(r.getFinancialDischargeDate()).deltaDaysVsActual(r.getDeltaDaysVsActual())
        .achievementRate(r.getAchievementRate()).comment(r.getComment()).recommendedScenario(r.getRecommendedScenario())
        .baselineDischargeDate(r.getBaselineDischargeDate()).deltaDaysVsBaseline(r.getDeltaDaysVsBaseline()).pros(r.getPros()).cons(r.getCons()).build();
  }

  private String comment(AiAnalysisInput base, long expected, boolean scenario) {
    long shortfall = Math.max(0, base.targetAmount() - expected);
    if (!scenario) return shortfall == 0 ? "현재 페이스라면 목표 자산 달성이 예상됩니다. 소비 흐름을 유지해보세요." : "현재 페이스의 전역 예상 자산은 " + money(expected) + "원이며, 목표까지 " + money(shortfall) + "원이 더 필요합니다.";
    long difference = expected - base.expectedAsset();
    return "이 시나리오는 평소 예상 자산 대비 " + money(Math.abs(difference)) + "원 " + (difference >= 0 ? "증가" : "감소") + "으로 계산됩니다." + (shortfall == 0 ? " 목표 달성이 예상됩니다." : " 목표까지 추가 관리가 필요합니다.");
  }
  private String reason(AiAnalysisInput base, long saving, long spending) {
    long shortfall = Math.max(0, base.targetAmount() - base.expectedAsset());
    return shortfall == 0 ? "현재 목표 달성 페이스를 유지할 수 있도록 소비 한도를 관리하는 추천입니다." : "목표까지 " + money(shortfall) + "원이 부족해 월 저축 " + money(saving) + "원, 월 소비 " + money(spending) + "원 이하를 제안합니다.";
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
    long analysisExpectedAsset = simulation == null ? base.expectedAsset() : simulation.getExpectedAsset();
    return "[분석 기준] " + (simulation == null ? "평소 자산 흐름" : "What-if 시뮬레이션 비교") + "\n"
        + "목표 자산: " + money(base.targetAmount()) + "원\n"
        + "분석 예상 자산: " + money(analysisExpectedAsset) + "원\n"
        + "목표 달성률: " + result.getAchievementRate() + "%\n"
        + "재정적 전역일: " + result.getFinancialDischargeDate() + "\n"
        + "실제 전역일 대비 차이: " + result.getDeltaDaysVsActual() + "일\n"
        + "추천 월 저축액: " + money(recommended.getMonthlySavingAmount()) + "원\n"
        + "추천 월 소비 한도: " + money(recommended.getMonthlySpendingAmount()) + "원\n"
        + "추천 투자 비율: " + recommended.getInvestmentRatio() + "%\n"
        + "추천 기대수익률: " + recommended.getExpectedReturnRate() + "%\n"
        + "위 확정값을 변경하거나 새 숫자를 만들지 말고 comment에는 결과 해석, recommendReason에는 실행 시 유의점을 작성하세요.";
  }
  private String hash(AiAnalysisInput b, SimulationVo s, OpenAiModel model) {
    String raw = "analysis-v2|" + PROMPT_VERSION + "|" + model.apiName() + "|" + b.forecastId() + "|" + b.snapshotId() + "|" + b.targetAmount() + "|" + b.expectedAsset() + "|" + (s == null ? "baseline" : s.getSimulationId() + "|" + s.getMonthlySavingAmount() + "|" + s.getInvestmentRatio() + "|" + s.getExpectedReturnRate() + "|" + s.getMonthlySpendingAmount() + "|" + s.getExpectedAsset());
    try { return HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(raw.getBytes(StandardCharsets.UTF_8))); }
    catch (Exception e) { throw new IllegalStateException("AI 분석 입력 해시를 생성할 수 없습니다.", e); }
  }
  private AiAnalysisResult read(String json) { try { return objectMapper.readValue(json, AiAnalysisResult.class); } catch (JsonProcessingException e) { throw new AiAnalysisException(AiAnalysisErrorCode.RESULT_UNREADABLE, "저장된 AI 분석 결과를 읽을 수 없습니다.", e); } }
  private String write(AiAnalysisResult result) { try { return objectMapper.writeValueAsString(result); } catch (JsonProcessingException e) { throw new AiAnalysisException(AiAnalysisErrorCode.RESULT_UNREADABLE, "AI 분석 결과를 저장할 수 없습니다.", e); } }
  private long remainingMonths(LocalDate today, LocalDate discharge) { YearMonth first = YearMonth.from(today).plusMonths(1); YearMonth last = YearMonth.from(discharge); return first.isAfter(last) ? 0 : ChronoUnit.MONTHS.between(first, last) + 1; }
  private long ceil(long value, long divisor) { return (value + divisor - 1) / divisor; }
  private BigDecimal rate(long expected, long target) { return target == 0 ? new BigDecimal("100.00") : BigDecimal.valueOf(expected).multiply(BigDecimal.valueOf(100)).divide(BigDecimal.valueOf(target), 2, RoundingMode.HALF_UP); }
  private Integer days(LocalDate start, LocalDate end) { return start == null || end == null ? null : Math.toIntExact(ChronoUnit.DAYS.between(start, end)); }
  private String money(long value) { return String.format(Locale.KOREA, "%,d", value); }
}
