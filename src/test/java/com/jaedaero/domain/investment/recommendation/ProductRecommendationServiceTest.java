package com.jaedaero.domain.investment.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import com.jaedaero.domain.auth.mapper.InvestmentPreferenceMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.investment.etf.EtfDailyTradingInfo;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewItem;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewResponse;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewService;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class ProductRecommendationServiceTest {

  @Test
  void excludesLeveragedEtfAndPenalizesExistingPlanAndUnaffordablePrice() {
    EtfMarketOverviewService overviewService =
        new EtfMarketOverviewService(null) {
          @Override
          public EtfMarketOverviewResponse getOverview(LocalDate date) {
            return new EtfMarketOverviewResponse(
                "20260807",
                "20260807",
                List.of(
                    item("069500", "KODEX 200", "50000", "50000", "1000", "10000", "코스피 200"),
                    item("122630", "KODEX 레버리지", "10000", "10000", "100000", "100000", "코스피 200"),
                    item("999999", "해외 주식", "500000", "499000", "500", "5000", "MSCI World")));
          }
        };
    RecurringInvestmentPlanVo plan =
        RecurringInvestmentPlanVo.builder()
            .maximumMonthlyAmount(100_000L)
            .investmentProductCode("069500")
            .build();
    ProductRecommendationService service =
        new ProductRecommendationService(
            overviewService,
            new EtfRiskClassifier(),
            preferenceMapper(InvestmentPreference.AGGRESSIVE),
            planMapper(plan),
            userId -> LocalDate.now().plusDays(90));

    ProductRecommendationResponse response = service.getEtfRecommendations(1L, LocalDate.now());

    assertEquals(InvestmentPreference.AGGRESSIVE, response.investmentPreference());
    assertEquals(100_000L, response.monthlyInvestmentBudget());
    assertFalse(response.personalizedRecommendations().stream().anyMatch(item -> item.isuCd().equals("122630")));
    PersonalizedEtfRecommendation held = response.personalizedRecommendations().stream()
        .filter(item -> item.isuCd().equals("069500")).findFirst().orElseThrow();
    assertEquals(-75, held.scoreBreakdown().adjustmentScore());
    assertEquals(15, held.scoreBreakdown().liquidityScore());
    assertEquals(10, held.scoreBreakdown().navGapScore());
    assertEquals(10, held.scoreBreakdown().sizeScore());
    assertEquals(25, held.suitabilityScore());
  }

  @Test
  void returnsNoPersonalizedRecommendationsWhenPreferenceOrPlanIsMissing() {
    EtfMarketOverviewService overviewService = overviewService("20260807");

    ProductRecommendationResponse missingPreference =
        new ProductRecommendationService(
                overviewService, new EtfRiskClassifier(), preferenceMapper(null), planMapper(plan("999999")), userId -> null)
            .getEtfRecommendations(1L, LocalDate.of(2026, 8, 7));
    ProductRecommendationResponse missingPlan =
        new ProductRecommendationService(
                overviewService, new EtfRiskClassifier(), preferenceMapper(InvestmentPreference.AGGRESSIVE), planMapper(null), userId -> null)
            .getEtfRecommendations(1L, LocalDate.of(2026, 8, 7));

    assertTrue(missingPreference.personalizedRecommendations().isEmpty());
    assertTrue(missingPlan.personalizedRecommendations().isEmpty());
  }

  @Test
  void usesActualMarketDateForHistoricalRecommendationAdjustment() {
    RecurringInvestmentPlanVo plan = plan("999999");
    ProductRecommendationService service =
        new ProductRecommendationService(
            overviewService("20250101"),
            new EtfRiskClassifier(),
            preferenceMapper(InvestmentPreference.AGGRESSIVE),
            planMapper(plan),
            userId -> LocalDate.of(2025, 12, 1));

    PersonalizedEtfRecommendation recommendation =
        service.getEtfRecommendations(1L, LocalDate.of(2025, 1, 1)).personalizedRecommendations().get(0);

    assertEquals(-15, recommendation.scoreBreakdown().adjustmentScore());
  }

  @Test
  void usesAnalysisContextBudgetInsteadOfCurrentPlanBudget() {
    ProductRecommendationService service =
        new ProductRecommendationService(
            overviewService("20260807"),
            new EtfRiskClassifier(),
            preferenceMapper(InvestmentPreference.AGGRESSIVE),
            planMapper(plan("999999")),
            userId -> LocalDate.of(2027, 1, 1));

    ProductRecommendationResponse response =
        service.getEtfRecommendations(
            1L,
            LocalDate.of(2026, 8, 7),
            new ProductRecommendationContext(
                11L,
                7L,
                30_000L,
                new BigDecimal("6.50"),
                LocalDate.of(2026, 12, 1)));

    assertEquals(30_000L, response.monthlyInvestmentBudget());
    assertEquals(11L, response.analysisId());
    assertEquals(7L, response.simulationId());
    assertEquals(new BigDecimal("6.50"), response.expectedReturnRate());
    assertEquals(30_000L, response.personalizedRecommendations().get(0).recommendedMonthlyAmount());
  }

  @Test
  void usesWhatIfExpectedReturnRateToSetRecommendationAllocation() {
    EtfMarketOverviewService overviewService =
        new EtfMarketOverviewService(null) {
          @Override
          public EtfMarketOverviewResponse getOverview(LocalDate date) {
            return new EtfMarketOverviewResponse(
                "20260807",
                "20260807",
                List.of(
                    item("100001", "단기국채 ETF", "10000", "10000", "1000", "10000", "국고채"),
                    item("100002", "코스피 200", "10000", "10000", "1000", "10000", "코스피 200"),
                    item("100003", "미국 S&P 500", "10000", "10000", "1000", "9000", "S&P 500"),
                    item("100004", "글로벌 주식", "10000", "10000", "1000", "8000", "MSCI World"),
                    item("100005", "반도체 주식", "10000", "10000", "1000", "7000", "반도체")));
          }
        };
    ProductRecommendationService service =
        new ProductRecommendationService(
            overviewService,
            new EtfRiskClassifier(),
            preferenceMapper(InvestmentPreference.AGGRESSIVE),
            planMapper(null),
            userId -> LocalDate.of(2028, 1, 1));

    ProductRecommendationResponse response =
        service.getEtfRecommendations(
            1L,
            LocalDate.of(2026, 8, 7),
            new ProductRecommendationContext(
                null, 7L, 100_000L, new BigDecimal("9.00"), LocalDate.of(2028, 1, 1)));

    assertEquals(30, response.recommendedAllocation().safePercentage());
    assertEquals(70, response.recommendedAllocation().riskPercentage());
    assertEquals(
        4,
        response.personalizedRecommendations().stream()
            .filter(item -> item.assetBucket() == AssetBucket.RISK)
            .count());
  }

  private static EtfMarketOverviewService overviewService(String marketDate) {
    return new EtfMarketOverviewService(null) {
      @Override
      public EtfMarketOverviewResponse getOverview(LocalDate date) {
        return new EtfMarketOverviewResponse(
            marketDate,
            marketDate,
            List.of(item("069500", "KODEX 200", "50000", "50000", "1000", "10000", "코스피 200")));
      }
    };
  }

  private static RecurringInvestmentPlanVo plan(String investmentProductCode) {
    return RecurringInvestmentPlanVo.builder()
        .maximumMonthlyAmount(100_000L)
        .investmentProductCode(investmentProductCode)
        .build();
  }

  private static EtfMarketOverviewItem item(
      String code, String name, String price, String nav, String tradingValue, String marketCap, String indexName) {
    return new EtfMarketOverviewItem(
        new EtfDailyTradingInfo("20260807", code, name, price, null, null, nav, null, null, null,
            null, tradingValue, marketCap, null, null, indexName, null, null, null),
        List.of());
  }

  private static InvestmentPreferenceMapper preferenceMapper(InvestmentPreference preference) {
    return new InvestmentPreferenceMapper() {
      @Override public int countActiveUserByUserId(long userId) { return 1; }
      @Override public InvestmentPreference findInitialPreferenceByUserId(long userId) { return preference; }
      @Override public void upsertInitialPreference(long userId, InvestmentPreference preference) {}
      @Override public void upsertGoalTargetAmount(long userId, long targetAmount) {}
    };
  }

  private static RecurringInvestmentPlanMapper planMapper(RecurringInvestmentPlanVo plan) {
    return new RecurringInvestmentPlanMapper() {
      @Override public int insert(RecurringInvestmentPlanVo value) { return 0; }
      @Override public int update(RecurringInvestmentPlanVo value) { return 0; }
      @Override public RecurringInvestmentPlanVo findByUserId(long userId) { return plan; }
      @Override public RecurringInvestmentPlanVo findByIdAndUserId(long planId, long userId) { return plan; }
      @Override public boolean existsSecuritiesAccount(long accountId, long userId) { return true; }
    };
  }
}
