package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;

@ApiModel(description = "What-if 목표 수익률을 기준으로 계산한 ETF 추천 자산 배분")
public record RecommendationAllocation(
    @ApiModelProperty(value = "안전자산 권장 비중(%)", example = "50") int safePercentage,
    @ApiModelProperty(value = "위험자산 권장 비중(%)", example = "50") int riskPercentage) {}
