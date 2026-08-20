package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.util.List;

@ApiModel(description = "사용자 투자 조건을 반영한 ETF 추천 결과")
public record PersonalizedEtfRecommendation(
    @ApiModelProperty(value = "ETF 종목코드", example = "069500") String isuCd,
    @ApiModelProperty(value = "ETF 종목명", example = "KODEX 200") String name,
    @ApiModelProperty(value = "추천 자산 바구니", example = "RISK") AssetBucket assetBucket,
    @ApiModelProperty(value = "내부 위험등급", example = "HIGH") RiskLevel riskLevel,
    @ApiModelProperty(value = "적합도 점수", example = "87") int suitabilityScore,
    @ApiModelProperty(value = "적합도 점수 세부 내역") RecommendationScoreBreakdown scoreBreakdown,
    @ApiModelProperty(value = "월 추천 투자금", example = "300000") long recommendedMonthlyAmount,
    @ApiModelProperty(value = "추천 사유") List<String> reasons,
    @ApiModelProperty(value = "투자 유의사항") List<String> warnings) {}
