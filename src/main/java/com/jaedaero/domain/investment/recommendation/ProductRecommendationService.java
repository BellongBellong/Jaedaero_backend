package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import com.jaedaero.domain.auth.mapper.InvestmentPreferenceMapper;
import com.jaedaero.domain.dashboard.mapper.DashboardMapper;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewItem;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewResponse;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewService;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductRecommendationService {

  private final EtfMarketOverviewService marketOverviewService;
  private final EtfRiskClassifier riskClassifier;
  private final InvestmentPreferenceMapper investmentPreferenceMapper;
  private final RecurringInvestmentPlanMapper recurringInvestmentPlanMapper;
  private final DashboardMapper dashboardMapper;

  public ProductRecommendationService(
      EtfMarketOverviewService marketOverviewService,
      EtfRiskClassifier riskClassifier,
      InvestmentPreferenceMapper investmentPreferenceMapper,
      RecurringInvestmentPlanMapper recurringInvestmentPlanMapper,
      DashboardMapper dashboardMapper) {
    this.marketOverviewService = marketOverviewService;
    this.riskClassifier = riskClassifier;
    this.investmentPreferenceMapper = investmentPreferenceMapper;
    this.recurringInvestmentPlanMapper = recurringInvestmentPlanMapper;
    this.dashboardMapper = dashboardMapper;
  }

  public ProductRecommendationResponse getEtfRecommendations(long userId, LocalDate asOfDate) {
    EtfMarketOverviewResponse overview = marketOverviewService.getOverview(asOfDate);
    Map<RiskLevel, List<EtfProductRecommendation>> itemsByRiskLevel =
        overview.items().stream()
            .map(this::toRecommendation)
            .collect(Collectors.groupingBy(EtfProductRecommendation::riskLevel));

    List<RiskLevelRecommendationGroup> groups =
        List.of(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH).stream()
            .map(level -> group(level, itemsByRiskLevel.getOrDefault(level, List.of())))
            .toList();
    InvestmentPreference preference = investmentPreferenceMapper.findInitialPreferenceByUserId(userId);
    RecurringInvestmentPlanVo plan = recurringInvestmentPlanMapper.findByUserId(userId);
    List<PersonalizedEtfRecommendation> personalized =
        preference == null || plan == null
            ? List.of()
            : personalized(overview.items(), preference, plan, dashboardMapper.findActualDischargeDateByUserId(userId));
    return new ProductRecommendationResponse(
        overview.requestedAsOfDate(),
        overview.asOfDate(),
        "ETF명과 기초지수명의 자산군·구조 키워드로 분류한 내부 리밸런싱 규칙입니다. 운용사 공식 위험등급이 아닙니다.",
        groups,
        preference,
        plan == null ? null : plan.getMaximumMonthlyAmount(),
        personalized);
  }

  private List<PersonalizedEtfRecommendation> personalized(
      List<EtfMarketOverviewItem> items,
      InvestmentPreference preference,
      RecurringInvestmentPlanVo plan,
      LocalDate dischargeDate) {
    String heldIndex =
        items.stream()
            .map(EtfMarketOverviewItem::etf)
            .filter(etf -> sameCode(etf.isuCd(), plan.getInvestmentProductCode()))
            .map(com.jaedaero.domain.investment.etf.EtfDailyTradingInfo::idxIndNm)
            .findFirst()
            .orElse(null);
    return items.stream()
        .map(item -> personalized(item.etf(), preference, plan, heldIndex, dischargeDate))
        .filter(java.util.Objects::nonNull)
        .sorted(Comparator.comparingInt(PersonalizedEtfRecommendation::suitabilityScore).reversed())
        .limit(5)
        .toList();
  }

  private PersonalizedEtfRecommendation personalized(
      com.jaedaero.domain.investment.etf.EtfDailyTradingInfo etf,
      InvestmentPreference preference,
      RecurringInvestmentPlanVo plan,
      String heldIndex,
      LocalDate dischargeDate) {
    EtfRiskClassification classification = riskClassifier.classify(etf);
    if (!classification.eligibleForRecommendation()) return null;
    int score = riskScore(preference, classification.riskLevel());
    List<String> reasons = new ArrayList<>(classification.classificationReasons());
    List<String> warnings = new ArrayList<>();
    if (sameCode(etf.isuCd(), plan.getInvestmentProductCode())
        || sameIndex(etf.idxIndNm(), heldIndex)) {
      score -= 40;
      warnings.add("현재 적립 계획과 동일한 ETF입니다. 분산투자 관점에서 우선순위를 낮췄습니다.");
    }
    long budget = plan.getMaximumMonthlyAmount();
    long price = amount(etf.tddClsprc());
    if (price > budget) {
      score -= 30;
      warnings.add("1주 가격이 월 투자 목표금액보다 높아 분할 매수가 어렵습니다.");
    } else if (price * 2 > budget) {
      score -= 10;
      warnings.add("1주 가격이 월 투자 목표금액에 가까워 분할 매수 여유가 적습니다.");
    }
    if (dischargeDate != null && classification.riskLevel() == RiskLevel.HIGH) {
      long days = ChronoUnit.DAYS.between(LocalDate.now(), dischargeDate);
      if (days <= 180) {
        score -= 35;
        warnings.add("전역 예정일이 6개월 이내여서 고위험 ETF 비중을 보수적으로 반영했습니다.");
      } else if (days <= 365) {
        score -= 15;
        warnings.add("전역 예정일이 1년 이내여서 고위험 ETF 비중을 낮췄습니다.");
      }
    }
    reasons.add(preferenceReason(preference, classification.riskLevel()));
    return new PersonalizedEtfRecommendation(
        etf.isuCd(), etf.isuNm(), Math.max(0, score), budget, List.copyOf(reasons), List.copyOf(warnings));
  }

  private int riskScore(InvestmentPreference preference, RiskLevel riskLevel) {
    return switch (preference) {
      case SAFE -> riskLevel == RiskLevel.LOW ? 90 : riskLevel == RiskLevel.MEDIUM ? 70 : 25;
      case BALANCED -> riskLevel == RiskLevel.MEDIUM ? 90 : riskLevel == RiskLevel.HIGH ? 80 : 65;
      case AGGRESSIVE -> riskLevel == RiskLevel.HIGH ? 95 : riskLevel == RiskLevel.MEDIUM ? 70 : 45;
    };
  }

  private String preferenceReason(InvestmentPreference preference, RiskLevel riskLevel) {
    return preference + " 투자성향에 " + riskLevel + " 위험등급을 반영했습니다.";
  }

  private boolean sameCode(String first, String second) {
    return first != null && first.equalsIgnoreCase(second == null ? "" : second.trim());
  }

  private boolean sameIndex(String first, String second) {
    return first != null
        && second != null
        && !first.isBlank()
        && first.replaceAll("\\s+", "").equalsIgnoreCase(second.replaceAll("\\s+", ""));
  }

  private long amount(String value) {
    try {
      return value == null ? 0L : new BigDecimal(value.replace(",", "")).longValue();
    } catch (NumberFormatException exception) {
      return 0L;
    }
  }

  private RiskLevelRecommendationGroup group(
      RiskLevel riskLevel, List<EtfProductRecommendation> items) {
    List<EtfProductRecommendation> sorted =
        items.stream()
            .sorted(Comparator.comparing(this::marketCap).reversed())
            .toList();
    AssetBucket assetBucket =
        riskLevel == RiskLevel.VERY_HIGH
            ? AssetBucket.EXCLUDED
            : riskLevel == RiskLevel.HIGH ? AssetBucket.RISK : AssetBucket.SAFE;
    return new RiskLevelRecommendationGroup(
        riskLevel, assetBucket, riskLevel != RiskLevel.VERY_HIGH, sorted.size(), sorted);
  }

  private EtfProductRecommendation toRecommendation(EtfMarketOverviewItem item) {
    EtfRiskClassification classification = riskClassifier.classify(item.etf());
    return new EtfProductRecommendation(
        item.etf().isuCd(),
        item.etf().isuNm(),
        classification.riskLevel(),
        classification.assetBucket(),
        classification.eligibleForRecommendation(),
        classification.classificationReasons(),
        item.etf(),
        item.returns());
  }

  private BigDecimal marketCap(EtfProductRecommendation item) {
    String value = item.market().mktcap();
    if (value == null || value.isBlank() || "-".equals(value)) return BigDecimal.ZERO;
    try {
      return new BigDecimal(value.replace(",", ""));
    } catch (NumberFormatException exception) {
      return BigDecimal.ZERO;
    }
  }
}
