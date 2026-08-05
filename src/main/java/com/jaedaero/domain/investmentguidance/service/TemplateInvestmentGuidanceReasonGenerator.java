package com.jaedaero.domain.investmentguidance.service;

import com.jaedaero.domain.investmentguidance.vo.InvestmentGuidanceAction;
import java.text.NumberFormat;
import java.util.Locale;
import org.springframework.stereotype.Component;

@Component
public class TemplateInvestmentGuidanceReasonGenerator
    implements InvestmentGuidanceReasonGenerator {

  @Override
  public String generate(
      InvestmentGuidanceAction action,
      long currentContributionAmount,
      long recommendedContributionAmount,
      long expectedAsset,
      long targetAmount,
      String dataIssue) {
    if (action == InvestmentGuidanceAction.REVIEW) {
      return dataIssue == null
          ? "전역일까지 남은 적립 회차 또는 월 최대 투자한도를 확인한 뒤 다시 계산해 주세요."
          : "증권 평가 데이터를 확인하지 못해 기존 계획을 유지했습니다. 계좌 동기화 후 다시 계산해 주세요. ("
              + dataIssue
              + ")";
    }
    String amount = won(recommendedContributionAmount);
    String margin = won(Math.abs(expectedAsset - targetAmount));
    return switch (action) {
      case START -> "전역 목표에 가까워지도록 다음 회차부터 " + amount + " 적립을 시작해 보세요.";
      case CONTINUE -> "현재 적립이 목표 달성에 계속 필요하므로 회차당 " + amount + "을 유지해 주세요.";
      case REDUCE ->
          "회차당 " + amount + "으로 줄여도 목표 달성이 예상됩니다. 예상 목표 여유액은 " + margin + "입니다.";
      case PAUSE -> "신규 적립 없이도 목표는 예상되지만 여유가 크지 않아 이번 회차만 쉬고 다음 주기에 다시 점검합니다.";
      case SAFE_FOCUS ->
          "신규 위험자산 적립 없이도 목표와 1개월 안전 여유금이 예상됩니다. 보유자산은 매도하지 않고 앞으로 넣을 돈만 안전저축에 집중합니다.";
      case REVIEW -> throw new IllegalStateException("REVIEW는 위에서 처리됩니다.");
    };
  }

  private String won(long amount) {
    return NumberFormat.getNumberInstance(Locale.KOREA).format(amount) + "원";
  }
}
