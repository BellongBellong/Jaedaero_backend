package com.jaedaero.domain.aianalysis.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrative;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerationException;
import com.jaedaero.domain.aianalysis.llm.AiCoachNarrativeGenerator;
import com.jaedaero.domain.aianalysis.llm.AiGenerationTask;
import com.jaedaero.domain.aianalysis.llm.OpenAiModel;
import com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper;
import com.jaedaero.domain.aianalysis.service.impl.AiAnalysisServiceImpl;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.simulation.mapper.SimulationMapper;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import com.jaedaero.domain.simulation.vo.SimulationVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.Test;

class AiAnalysisServiceImplTest {

  @Test
  void sameBaselineInput_reusesStoredAnalysisAndRecommendation() {
    InMemoryAiAnalysisMapper mapper = new InMemoryAiAnalysisMapper();
    AiAnalysisInputProvider analysisInput =
        userId ->
            new AiAnalysisInput(
                10L,
                20L,
                18_150_000L,
                null,
                new BigDecimal("90.75"),
                20_000_000L,
                LocalDate.of(2027, 9, 1),
                180_000L,
                spendingPattern());
    com.jaedaero.domain.simulation.service.SimulationInputProvider simulationInput =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                180_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    AiAnalysisService service =
        new AiAnalysisServiceImpl(
            mapper,
            analysisInput,
            new EmptySimulationMapper(),
            simulationInput,
            simulationCalculator(),
            new ObjectMapper().registerModule(new JavaTimeModule()),
            (model, prompt) -> new AiCoachNarrative("생성된 AI 코치 문구입니다.", "생성된 추천 사유입니다."),
            new SpendingPatternAnalyzer());

    AiAnalysisResponse first = service.analyze(1L, new AiAnalysisRequest());
    AiAnalysisResponse second = service.analyze(1L, new AiAnalysisRequest());

    assertEquals(AiAnalysisType.DIAGNOSIS, first.getAnalysisType());
    assertEquals(first.getAnalysisId(), second.getAnalysisId());
    assertEquals(1, mapper.analyses.size());
    assertEquals(1, mapper.recommendations.size());
    assertNotNull(second.getRecommendedScenario().getScenarioId());
    assertEquals("생성된 AI 코치 문구입니다.", first.getComment());
    assertEquals("생성된 추천 사유입니다.", first.getRecommendedScenario().getRecommendReason());
    assertEquals("gpt-4o-mini", mapper.analyses.get(0).getModelName());
    assertEquals(AiGenerationSource.OPENAI, mapper.analyses.get(0).getGenerationSource());
    assertEquals(AiGenerationSource.OPENAI, first.getGenerationSource());
    assertEquals(AiGenerationSource.CACHE, second.getGenerationSource());
    assertEquals(117_700L, first.getSpendingPattern().getTotalSpendingAmount());
    assertEquals(20_000L, first.getSpendingImprovement().getSuggestedMonthlyReductionAmount());
    assertEquals(260_000L, first.getSpendingExpectedEffect().getExpectedAssetIncreaseAmount());
    assertEquals(
        first.getRecommendedScenario().getMonthlyInvestmentAmount(),
        mapper.recommendations.get(0).getMonthlyInvestmentAmount());
    AiAnalysisResponse detail = service.getDetail(1L, first.getAnalysisId());
    assertEquals(first.getAnalysisId(), detail.getAnalysisId());
    assertEquals(AiGenerationSource.OPENAI, detail.getGenerationSource());
    assertEquals(
        first.getRecommendedScenario().getMonthlyInvestmentAmount(),
        detail.getRecommendedScenario().getMonthlyInvestmentAmount());
  }

  @Test
  void generationTasksUseServerAssignedModels() {
    assertEquals(OpenAiModel.GPT_4O_MINI, AiGenerationTask.AI_COACH.model());
    assertEquals(OpenAiModel.GPT_5_NANO, AiGenerationTask.TRANSACTION_CATEGORY.model());
    assertEquals(OpenAiModel.GPT_5_NANO, AiGenerationTask.DAILY_MARKET_REPORT.model());
    assertEquals(OpenAiModel.GPT_4O_MINI, AiGenerationTask.DISCHARGE_REPORT.model());
  }

  @Test
  void simulationRecommendation_keepsMonthlyInvestmentAmountThroughDetail() {
    InMemoryAiAnalysisMapper mapper = new InMemoryAiAnalysisMapper();
    AiAnalysisInputProvider analysisInput =
        userId ->
            new AiAnalysisInput(
                10L,
                20L,
                18_150_000L,
                null,
                new BigDecimal("90.75"),
                20_000_000L,
                LocalDate.of(2027, 9, 1),
                180_000L,
                spendingPattern());
    com.jaedaero.domain.simulation.service.SimulationInputProvider simulationInput =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                180_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    SimulationVo simulation =
        SimulationVo.builder()
            .simulationId(9L)
            .userId(1L)
            .targetAmount(20_000_000L)
            .monthlySavingAmount(300_000L)
            .monthlyInvestmentAmount(150_000L)
            .expectedReturnRate(new BigDecimal("5.00"))
            .monthlySpendingAmount(180_000L)
            .expectedAsset(18_500_000L)
            .build();
    AtomicReference<String> generatedPrompt = new AtomicReference<>();
    AiAnalysisService service =
        new AiAnalysisServiceImpl(
            mapper,
            analysisInput,
            new SingleSimulationMapper(simulation),
            simulationInput,
            simulationCalculator(),
            new ObjectMapper().registerModule(new JavaTimeModule()),
            (model, prompt) -> {
              generatedPrompt.set(prompt);
              return new AiCoachNarrative("시나리오 분석입니다.", "월 투자금액을 유지하세요.");
            },
            new SpendingPatternAnalyzer());
    AiAnalysisRequest request = new AiAnalysisRequest();
    request.setSimulationId(9L);

    AiAnalysisResponse created = service.analyze(1L, request);
    AiAnalysisResponse detail = service.getDetail(1L, created.getAnalysisId());

    assertEquals(150_000L, created.getRecommendedScenario().getMonthlyInvestmentAmount());
    assertEquals(
        created.getRecommendedScenario().getMonthlyInvestmentAmount(),
        mapper.recommendations.get(0).getMonthlyInvestmentAmount());
    assertEquals(
        created.getRecommendedScenario().getMonthlyInvestmentAmount(),
        detail.getRecommendedScenario().getMonthlyInvestmentAmount());
    assertTrue(generatedPrompt.get().contains("추천 월 투자금액: 150,000원"));
    assertTrue(generatedPrompt.get().contains("반복 결제 확인 필요"));
    assertTrue(generatedPrompt.get().contains("넷플릭스"));
    assertFalse(generatedPrompt.get().contains("추천 투자 비율"));
    assertTrue(mapper.analyses.get(0).getResultJson().contains("monthlyInvestmentAmount"));
    assertFalse(mapper.analyses.get(0).getResultJson().contains("investmentRatio"));
  }

  @Test
  void narrativeFailure_isNotCachedAndNextAttemptUsesOpenAi() {
    InMemoryAiAnalysisMapper mapper = new InMemoryAiAnalysisMapper();
    AiAnalysisInputProvider analysisInput =
        userId -> new AiAnalysisInput(10L, 20L, 18_150_000L, null, new BigDecimal("90.75"), 20_000_000L, LocalDate.of(2027, 9, 1), 180_000L, spendingPattern());
    com.jaedaero.domain.simulation.service.SimulationInputProvider simulationInput =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                180_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    AtomicInteger generationAttempts = new AtomicInteger();
    AiCoachNarrativeGenerator recoveringGenerator =
        (model, prompt) -> {
          if (generationAttempts.getAndIncrement() == 0) {
            throw new AiCoachNarrativeGenerationException("network unavailable");
          }
          return new AiCoachNarrative("복구된 AI 코치 문구입니다.", "복구된 추천 사유입니다.");
        };
    AiAnalysisService service = new AiAnalysisServiceImpl(mapper, analysisInput, new EmptySimulationMapper(), simulationInput, simulationCalculator(), new ObjectMapper().registerModule(new JavaTimeModule()), recoveringGenerator, new SpendingPatternAnalyzer());

    AiAnalysisResponse fallback = service.analyze(1L, new AiAnalysisRequest());
    AiAnalysisResponse recovered = service.analyze(1L, new AiAnalysisRequest());
    AiAnalysisResponse cached = service.analyze(1L, new AiAnalysisRequest());

    assertNotNull(fallback.getComment());
    assertNotNull(fallback.getSpendingPattern());
    assertEquals(20_000L, fallback.getSpendingImprovement().getSuggestedMonthlyReductionAmount());
    assertEquals(AiGenerationSource.FALLBACK, fallback.getGenerationSource());
    assertEquals(
        "openai-chat-v4-spending-pattern", mapper.analyses.get(0).getPromptVersion());
    assertEquals(AiGenerationSource.FALLBACK, mapper.analyses.get(0).getGenerationSource());
    assertEquals(AiGenerationSource.OPENAI, recovered.getGenerationSource());
    assertEquals("복구된 AI 코치 문구입니다.", recovered.getComment());
    assertEquals(AiGenerationSource.CACHE, cached.getGenerationSource());
    assertEquals(recovered.getAnalysisId(), cached.getAnalysisId());
    assertEquals(2, generationAttempts.get());
    assertEquals(2, mapper.analyses.size());
    assertEquals(2, mapper.recommendations.size());
  }

  @Test
  void changedTransactionAggregate_producesANewAnalysisInsteadOfUsingCache() {
    InMemoryAiAnalysisMapper mapper = new InMemoryAiAnalysisMapper();
    AtomicReference<SpendingPatternSnapshot> pattern = new AtomicReference<>(spendingPattern());
    AiAnalysisInputProvider analysisInput =
        userId ->
            new AiAnalysisInput(
                10L,
                20L,
                18_150_000L,
                null,
                new BigDecimal("90.75"),
                20_000_000L,
                LocalDate.of(2027, 9, 1),
                180_000L,
                pattern.get());
    com.jaedaero.domain.simulation.service.SimulationInputProvider simulationInput =
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                180_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1));
    AiAnalysisService service =
        new AiAnalysisServiceImpl(
            mapper,
            analysisInput,
            new EmptySimulationMapper(),
            simulationInput,
            simulationCalculator(),
            new ObjectMapper().registerModule(new JavaTimeModule()),
            (model, prompt) -> new AiCoachNarrative("소비 분석", "추천 사유"),
            new SpendingPatternAnalyzer());

    AiAnalysisResponse first = service.analyze(1L, new AiAnalysisRequest());
    pattern.set(
        new SpendingPatternSnapshot(
            LocalDate.of(2026, 8, 1),
            LocalDate.of(2026, 8, 7),
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 7, 7),
            List.of(new SpendingCategoryMetric("FOOD", "식비", 90_000L, 62_400L, 5, 3)),
            List.of()));
    AiAnalysisResponse changed = service.analyze(1L, new AiAnalysisRequest());

    assertFalse(first.getAnalysisId().equals(changed.getAnalysisId()));
    assertEquals(AiGenerationSource.OPENAI, changed.getGenerationSource());
    assertEquals(2, mapper.analyses.size());
  }

  private static SimulationCalculator simulationCalculator() {
    MilitaryPayPolicyMapper payMapper =
        (soldierType, rankName, monthStart, monthEnd) ->
            switch (rankName) {
              case "이병" -> 750_000L;
              case "일병" -> 900_000L;
              case "상병" -> 1_200_000L;
              case "병장" -> 1_500_000L;
              default -> null;
            };
    return new SimulationCalculator(new DefaultMilitaryPayPolicy(payMapper));
  }

  private static SpendingPatternSnapshot spendingPattern() {
    return new SpendingPatternSnapshot(
        LocalDate.of(2026, 8, 1),
        LocalDate.of(2026, 8, 7),
        LocalDate.of(2026, 7, 1),
        LocalDate.of(2026, 7, 7),
        List.of(
            new SpendingCategoryMetric("FOOD", "식비", 82_400L, 62_400L, 4, 3),
            new SpendingCategoryMetric("LEISURE", "여가", 35_300L, 35_300L, 1, 1),
            new SpendingCategoryMetric("ASSET", "자산", 550_000L, 550_000L, 1, 1)),
        List.of(new RecurringPaymentMetric("넷플릭스", 17_000L, 17_000L, 1, 1)));
  }

  private static class EmptySimulationMapper implements SimulationMapper {
    @Override public int insert(SimulationVo simulation) { return 0; }
    @Override public SimulationVo findByIdAndUserId(long simulationId, long userId) { return null; }
    @Override public List<SimulationVo> findByUserId(long userId, int offset, int limit) { return List.of(); }
    @Override public long countByUserId(long userId) { return 0; }
  }

  private static class SingleSimulationMapper implements SimulationMapper {

    private final SimulationVo simulation;

    private SingleSimulationMapper(SimulationVo simulation) {
      this.simulation = simulation;
    }

    @Override
    public int insert(SimulationVo simulation) {
      return 0;
    }

    @Override
    public SimulationVo findByIdAndUserId(long simulationId, long userId) {
      return this.simulation.getSimulationId() == simulationId
              && this.simulation.getUserId() == userId
          ? this.simulation
          : null;
    }

    @Override
    public List<SimulationVo> findByUserId(long userId, int offset, int limit) {
      return List.of();
    }

    @Override
    public long countByUserId(long userId) {
      return 0;
    }
  }

  private static class InMemoryAiAnalysisMapper implements AiAnalysisMapper {
    private final List<AiAnalysisVo> analyses = new ArrayList<>();
    private final List<AiRecommendedScenarioVo> recommendations = new ArrayList<>();
    @Override public int insertAnalysis(AiAnalysisVo analysis) { analysis.setAnalysisId((long) analyses.size() + 1); analyses.add(analysis); return 1; }
    @Override public AiAnalysisVo findAnalysisByIdAndUserId(long id, long userId) { return analyses.stream().filter(a -> a.getAnalysisId() == id && a.getUserId() == userId).findFirst().orElse(null); }
    @Override public AiAnalysisVo findLatestSuccessfulByUserIdAndInputDataHash(long userId, String hash) { return analyses.stream().filter(a -> a.getUserId() == userId && a.getInputDataHash().equals(hash)).filter(a -> a.getGenerationSource() == AiGenerationSource.OPENAI).reduce((first, second) -> second).orElse(null); }
    @Override public int insertRecommendedScenario(AiRecommendedScenarioVo scenario) { scenario.setScenarioId((long) recommendations.size() + 1); recommendations.add(scenario); return 1; }
    @Override public AiRecommendedScenarioVo findRecommendedScenarioByIdAndUserId(long id, long userId) { return recommendations.stream().filter(s -> s.getScenarioId() == id && s.getUserId() == userId).findFirst().orElse(null); }
  }
}
