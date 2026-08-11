package com.jaedaero.domain.investment.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import com.jaedaero.domain.auth.mapper.InvestmentPreferenceMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.investment.etf.EtfDailyTradingInfo;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewItem;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewResponse;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewService;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
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
                    item("069500", "KODEX 200", "50000", "코스피 200"),
                    item("122630", "KODEX 레버리지", "10000", "코스피 200"),
                    item("999999", "해외 주식", "500000", "MSCI World")));
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
            preferenceMapper(),
            planMapper(plan),
            userId -> LocalDate.now().plusDays(90));

    ProductRecommendationResponse response = service.getEtfRecommendations(1L, LocalDate.now());

    assertEquals(InvestmentPreference.AGGRESSIVE, response.investmentPreference());
    assertEquals(100_000L, response.monthlyInvestmentBudget());
    assertFalse(response.personalizedRecommendations().stream().anyMatch(item -> item.isuCd().equals("122630")));
    PersonalizedEtfRecommendation held = response.personalizedRecommendations().stream()
        .filter(item -> item.isuCd().equals("069500")).findFirst().orElseThrow();
    assertEquals(-75, held.scoreBreakdown().adjustmentScore());
    assertEquals(0, held.scoreBreakdown().liquidityScore());
    assertEquals(0, held.scoreBreakdown().navGapScore());
  }

  private static EtfMarketOverviewItem item(String code, String name, String price, String indexName) {
    return new EtfMarketOverviewItem(
        new EtfDailyTradingInfo("20260807", code, name, price, null, null, null, null, null, null,
            null, null, null, null, null, indexName, null, null, null),
        List.of());
  }

  private static InvestmentPreferenceMapper preferenceMapper() {
    return new InvestmentPreferenceMapper() {
      @Override public int countActiveUserByUserId(long userId) { return 1; }
      @Override public InvestmentPreference findInitialPreferenceByUserId(long userId) { return InvestmentPreference.AGGRESSIVE; }
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
