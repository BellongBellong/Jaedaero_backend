package com.jaedaero.domain.investment.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.investment.etf.EtfDailyTradingInfo;
import org.junit.jupiter.api.Test;

class EtfRiskClassifierTest {

  private final EtfRiskClassifier classifier = new EtfRiskClassifier();

  @Test
  void classifiesMoneyMarketEtfAsSafeLowRisk() {
    EtfRiskClassification result = classifier.classify(etf("KODEX KOFR금리액티브", "KOFR 지수"));

    assertEquals(RiskLevel.LOW, result.riskLevel());
    assertEquals(AssetBucket.SAFE, result.assetBucket());
    assertTrue(result.eligibleForRecommendation());
  }

  @Test
  void excludesLeverageAndInverseEtfFromInitialRecommendations() {
    EtfRiskClassification result = classifier.classify(etf("KODEX 200선물인버스2X", "코스피200 선물"));

    assertEquals(RiskLevel.VERY_HIGH, result.riskLevel());
    assertEquals(AssetBucket.EXCLUDED, result.assetBucket());
    assertFalse(result.eligibleForRecommendation());
  }

  @Test
  void classifiesBroadEquityEtfAsRiskAsset() {
    EtfRiskClassification result = classifier.classify(etf("KODEX 200", "코스피 200"));

    assertEquals(RiskLevel.HIGH, result.riskLevel());
    assertEquals(AssetBucket.RISK, result.assetBucket());
  }

  @Test
  void classifiesOnlyOneYearGovernmentBondAsLowRisk() {
    assertEquals(RiskLevel.LOW, classifier.classify(etf("국고채 1년", "국고채 1년 지수")).riskLevel());
    assertEquals(RiskLevel.LOW, classifier.classify(etf("국고채1년", "국고채1년 지수")).riskLevel());
    assertEquals(RiskLevel.MEDIUM, classifier.classify(etf("국고채 10년", "국고채 10년 지수")).riskLevel());
  }

  private EtfDailyTradingInfo etf(String name, String indexName) {
    return new EtfDailyTradingInfo(
        "20260806",
        "069500",
        name,
        "50000",
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        "1000000000000",
        null,
        null,
        indexName,
        null,
        null,
        null);
  }
}
