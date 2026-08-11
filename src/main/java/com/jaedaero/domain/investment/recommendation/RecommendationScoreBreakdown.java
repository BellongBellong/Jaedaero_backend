package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "ETF 추천 점수의 구성 항목")
public record RecommendationScoreBreakdown(
    @ApiModelProperty(value = "사용자 목표 자산군과의 적합도", example = "40") int assetAllocationScore,
    @ApiModelProperty(value = "사용자 투자성향과 ETF 위험등급의 적합도", example = "25") int riskLevelScore,
    @ApiModelProperty(value = "거래대금 기준 유동성 점수", example = "14") int liquidityScore,
    @ApiModelProperty(value = "종가와 NAV 괴리율 기준 점수", example = "9") int navGapScore,
    @ApiModelProperty(value = "시가총액 기준 규모 점수", example = "8") int sizeScore,
    @ApiModelProperty(value = "기존 보유·예산·전역일 조건에 따른 차감 점수", example = "-10") int adjustmentScore) {

  public int totalBeforeAdjustment() {
    return assetAllocationScore + riskLevelScore + liquidityScore + navGapScore + sizeScore;
  }
}
