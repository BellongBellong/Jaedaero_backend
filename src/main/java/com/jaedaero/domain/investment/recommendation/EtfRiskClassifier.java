package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.domain.investment.etf.EtfDailyTradingInfo;
import java.util.List;
import java.util.Locale;
import org.springframework.stereotype.Component;

/**
 * A transparent initial classifier based on KRX product and underlying-index names.
 * It is an internal allocation policy, not a fund manager's official risk rating.
 */
@Component
public class EtfRiskClassifier {

  public EtfRiskClassification classify(EtfDailyTradingInfo etf) {
    String text = normalizedText(etf);

    if (containsAny(text, "레버리지", "인버스", "곱버스", "2x", "3x", "vix", "변동성", "선물")) {
      return new EtfRiskClassification(
          RiskLevel.VERY_HIGH,
          AssetBucket.EXCLUDED,
          false,
          List.of("레버리지·인버스·파생형 성격의 ETF는 초기 리밸런싱 추천에서 제외합니다."));
    }
    if (containsAny(text, "mmf", "머니마켓", "kofr", "cd금리", "통안채", "단기국채", "국고채 1", "국고채1", "초단기")) {
      return new EtfRiskClassification(
          RiskLevel.LOW,
          AssetBucket.SAFE,
          true,
          List.of("현금성·초단기 또는 단기 국채 중심 ETF로 안전자산 바구니에 분류했습니다."));
    }
    if (containsAny(text, "채권", "국고채", "회사채", "혼합", "배당", "리츠", "금현물", "gold")) {
      return new EtfRiskClassification(
          RiskLevel.MEDIUM,
          AssetBucket.SAFE,
          true,
          List.of("채권·인컴·대체자산 성격을 반영해 안전자산 바구니에 분류했습니다."));
    }
    return new EtfRiskClassification(
        RiskLevel.HIGH,
        AssetBucket.RISK,
        true,
        List.of("주식형 또는 시장 추종 ETF로 위험자산 바구니에 분류했습니다."));
  }

  private String normalizedText(EtfDailyTradingInfo etf) {
    return ((etf.isuNm() == null ? "" : etf.isuNm())
            + " "
            + (etf.idxIndNm() == null ? "" : etf.idxIndNm()))
        .toLowerCase(Locale.ROOT)
        .replaceAll("\\s+", " ");
  }

  private boolean containsAny(String text, String... keywords) {
    for (String keyword : keywords) {
      if (text.contains(keyword)) return true;
    }
    return false;
  }
}
