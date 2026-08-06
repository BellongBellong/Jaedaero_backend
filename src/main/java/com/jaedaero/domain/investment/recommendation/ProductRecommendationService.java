package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.domain.investment.etf.EtfMarketOverviewItem;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewResponse;
import com.jaedaero.domain.investment.etf.EtfMarketOverviewService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class ProductRecommendationService {

  private final EtfMarketOverviewService marketOverviewService;
  private final EtfRiskClassifier riskClassifier;

  public ProductRecommendationService(
      EtfMarketOverviewService marketOverviewService, EtfRiskClassifier riskClassifier) {
    this.marketOverviewService = marketOverviewService;
    this.riskClassifier = riskClassifier;
  }

  public ProductRecommendationResponse getEtfRecommendations(LocalDate asOfDate) {
    EtfMarketOverviewResponse overview = marketOverviewService.getOverview(asOfDate);
    Map<RiskLevel, List<EtfProductRecommendation>> itemsByRiskLevel =
        overview.items().stream()
            .map(this::toRecommendation)
            .collect(Collectors.groupingBy(EtfProductRecommendation::riskLevel));

    List<RiskLevelRecommendationGroup> groups =
        List.of(RiskLevel.LOW, RiskLevel.MEDIUM, RiskLevel.HIGH, RiskLevel.VERY_HIGH).stream()
            .map(level -> group(level, itemsByRiskLevel.getOrDefault(level, List.of())))
            .toList();
    return new ProductRecommendationResponse(
        overview.requestedAsOfDate(),
        overview.asOfDate(),
        "ETF명과 기초지수명의 자산군·구조 키워드로 분류한 내부 리밸런싱 규칙입니다. 운용사 공식 위험등급이 아닙니다.",
        groups);
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
