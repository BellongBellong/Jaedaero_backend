package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import java.util.List;

@ApiModel(description = "KRX ETF 위험등급별 상품 분류 및 맞춤 추천 응답")
public record ProductRecommendationResponse(
    @ApiModelProperty(value = "요청한 기준일", example = "20260806") String requestedAsOfDate,
    @ApiModelProperty(value = "실제 적용 거래일", example = "20260805") String asOfDate,
    @ApiModelProperty(value = "분류 규칙 설명") String classificationPolicy,
    @ApiModelProperty(value = "위험등급별 ETF 목록") List<RiskLevelRecommendationGroup> groups,
    @ApiModelProperty(value = "사용자 투자 성향") InvestmentPreference investmentPreference,
    @ApiModelProperty(value = "월 투자 목표 금액") Long monthlyInvestmentBudget,
    @ApiModelProperty(value = "사용자 맞춤 상위 ETF 추천") List<PersonalizedEtfRecommendation> personalizedRecommendations) {}
