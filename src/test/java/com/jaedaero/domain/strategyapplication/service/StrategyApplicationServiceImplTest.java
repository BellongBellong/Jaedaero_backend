package com.jaedaero.domain.strategyapplication.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResult;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
import com.jaedaero.domain.aianalysis.mapper.AiAnalysisMapper;
import com.jaedaero.domain.aianalysis.service.AiAnalysisInput;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisVo;
import com.jaedaero.domain.aianalysis.vo.AiRecommendedScenarioVo;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.service.CashflowService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.domain.strategyapplication.exception.StrategyApplicationException;
import com.jaedaero.domain.strategyapplication.mapper.StrategyApplicationMapper;
import com.jaedaero.domain.strategyapplication.service.impl.StrategyApplicationServiceImpl;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;
import org.junit.jupiter.api.Test;

class StrategyApplicationServiceImplTest {

  @Test
  void appliesOwnedAiRecommendationAndReturnsNewestHistoryFirst() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    InMemoryAiAnalysisMapper aiAnalysisMapper = new InMemoryAiAnalysisMapper();
    aiAnalysisMapper.recommendation = recommendation(7L, 1L, 16_500_000L);
    aiAnalysisMapper.analysis = analysis(3L, 1L, 7L, objectMapper);
    InMemoryStrategyApplicationMapper applicationMapper =
        new InMemoryStrategyApplicationMapper();
    AtomicLong currentExpectedAsset = new AtomicLong(15_000_000L);
    SequencedCashflowService cashflowService =
        new SequencedCashflowService(
            currentExpectedAsset, List.of(16_500_000L, 17_200_000L));
    StrategyApplicationService service =
        new StrategyApplicationServiceImpl(
            applicationMapper,
            aiAnalysisMapper,
            userId ->
                new AiAnalysisInput(
                    10L,
                    20L,
                    currentExpectedAsset.get(),
                    null,
                    new BigDecimal("75.00"),
                    20_000_000L,
                    LocalDate.of(2027, 9, 1),
                    180_000L),
            cashflowService,
            objectMapper);

    StrategyApplicationResponse first = service.applyAiRecommendation(1L, 3L);
    aiAnalysisMapper.recommendation = recommendation(8L, 1L, 17_200_000L);
    aiAnalysisMapper.analysis = analysis(4L, 1L, 8L, objectMapper);
    StrategyApplicationResponse second = service.applyAiRecommendation(1L, 4L);

    assertEquals(StrategyApplicationSourceType.AI_RECOMMENDATION, first.getSourceType());
    assertEquals(7L, first.getAiScenarioId());
    assertNull(first.getSimulationId());
    assertEquals(300_000L, first.getAppliedMonthlySavingAmount());
    assertEquals(new BigDecimal("20.00"), first.getAppliedInvestmentRatio());
    assertEquals(new BigDecimal("5.00"), first.getAppliedExpectedReturnRate());
    assertEquals(180_000L, first.getAppliedMonthlySpendingAmount());
    assertEquals(15_000_000L, first.getBeforeExpectedAsset());
    assertEquals(16_500_000L, first.getAfterExpectedAsset());
    assertEquals(16_500_000L, second.getBeforeExpectedAsset());
    assertEquals(17_200_000L, second.getAfterExpectedAsset());
    assertEquals(2, cashflowService.generateCount);

    List<StrategyApplicationResponse> history = service.getHistory(1L, 0, 1);
    assertEquals(1, history.size());
    assertEquals(second.getApplicationId(), history.get(0).getApplicationId());
  }

  @Test
  void rejectsAnalysisOwnedByAnotherUser() throws Exception {
    ObjectMapper objectMapper = new ObjectMapper().registerModule(new JavaTimeModule());
    InMemoryAiAnalysisMapper aiAnalysisMapper = new InMemoryAiAnalysisMapper();
    aiAnalysisMapper.recommendation = recommendation(7L, 2L, 16_500_000L);
    aiAnalysisMapper.analysis = analysis(3L, 2L, 7L, objectMapper);
    StrategyApplicationService service =
        new StrategyApplicationServiceImpl(
            new InMemoryStrategyApplicationMapper(),
            aiAnalysisMapper,
            userId -> {
              throw new AssertionError("소유권 검증 전에 입력을 조회하면 안 됩니다.");
            },
            new SequencedCashflowService(new AtomicLong(), List.of()),
            objectMapper);

    assertThrows(
        StrategyApplicationException.class,
        () -> service.applyAiRecommendation(1L, 3L));
  }

  private AiAnalysisVo analysis(
      long analysisId, long userId, long scenarioId, ObjectMapper objectMapper) throws Exception {
    AiAnalysisResult result =
        AiAnalysisResult.builder()
            .recommendedScenario(
                AiRecommendedScenarioResponse.builder().scenarioId(scenarioId).build())
            .build();
    return AiAnalysisVo.builder()
        .analysisId(analysisId)
        .userId(userId)
        .resultJson(objectMapper.writeValueAsString(result))
        .build();
  }

  private AiRecommendedScenarioVo recommendation(long scenarioId, long userId, long expectedAsset) {
    return AiRecommendedScenarioVo.builder()
        .scenarioId(scenarioId)
        .userId(userId)
        .monthlySavingAmount(300_000L)
        .investmentRatio(new BigDecimal("20.00"))
        .expectedReturnRate(new BigDecimal("5.00"))
        .monthlySpendingAmount(180_000L)
        .expectedAsset(expectedAsset)
        .build();
  }

  private static class InMemoryAiAnalysisMapper implements AiAnalysisMapper {

    private AiAnalysisVo analysis;
    private AiRecommendedScenarioVo recommendation;

    @Override
    public int insertAnalysis(AiAnalysisVo analysis) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AiAnalysisVo findAnalysisByIdAndUserId(long analysisId, long userId) {
      return analysis != null
              && analysis.getAnalysisId() == analysisId
              && analysis.getUserId() == userId
          ? analysis
          : null;
    }

    @Override
    public AiAnalysisVo findLatestSuccessfulByUserIdAndInputDataHash(long userId, String hash) {
      throw new UnsupportedOperationException();
    }

    @Override
    public int insertRecommendedScenario(AiRecommendedScenarioVo scenario) {
      throw new UnsupportedOperationException();
    }

    @Override
    public AiRecommendedScenarioVo findRecommendedScenarioByIdAndUserId(
        long scenarioId, long userId) {
      return recommendation != null
              && recommendation.getScenarioId() == scenarioId
              && recommendation.getUserId() == userId
          ? recommendation
          : null;
    }
  }

  private static class SequencedCashflowService implements CashflowService {

    private final AtomicLong currentExpectedAsset;
    private final List<Long> generatedExpectedAssets;
    private int generateCount;

    private SequencedCashflowService(
        AtomicLong currentExpectedAsset, List<Long> generatedExpectedAssets) {
      this.currentExpectedAsset = currentExpectedAsset;
      this.generatedExpectedAssets = generatedExpectedAssets;
    }

    @Override
    public CashflowForecastResponse generate(long userId) {
      long expectedAsset = generatedExpectedAssets.get(generateCount++);
      currentExpectedAsset.set(expectedAsset);
      return CashflowForecastResponse.builder().expectedAsset(expectedAsset).build();
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      return CashflowForecastResponse.builder()
          .expectedAsset(currentExpectedAsset.get())
          .build();
    }

    @Override
    public CashflowForecastResponse getLatest(long userId, int months) {
      return getLatest(userId);
    }
  }

  private static class InMemoryStrategyApplicationMapper
      implements StrategyApplicationMapper {

    private final List<StrategyApplicationVo> applications = new ArrayList<>();

    @Override
    public int insert(StrategyApplicationVo application) {
      application.setApplicationId((long) applications.size() + 1);
      application.setAppliedAt(LocalDateTime.of(2026, 8, 3, 12, applications.size()));
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
          .filter(
              application ->
                  application.getApplicationId() == applicationId
                      && application.getUserId() == userId)
          .findFirst()
          .orElse(null);
    }

    @Override
    public StrategyApplicationVo findLatestByUserId(long userId) {
      return applications.stream()
          .filter(application -> application.getUserId() == userId)
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
              application ->
                  application.getUserId() == userId
                      && java.util.Objects.equals(application.getGuidanceId(), guidanceId)
                      && application.getAppliedGuidanceAction() == action
                      && application.getAppliedInvestmentFrequency() == frequency
                      && java.util.Objects.equals(
                          application.getAppliedRecurringContributionAmount(), contributionAmount))
          .findFirst()
          .orElse(null);
    }

    @Override
    public List<StrategyApplicationVo> findByUserId(
        long userId, long offset, int limit) {
      return applications.stream()
          .filter(application -> application.getUserId() == userId)
          .sorted(Comparator.comparing(StrategyApplicationVo::getApplicationId).reversed())
          .skip(offset)
          .limit(limit)
          .toList();
    }
  }
}
