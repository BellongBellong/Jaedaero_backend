package com.jaedaero.domain.rebalancing.vo;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RebalancingRecommendationVo {

  private Long rebalancingId;
  private Long userId;
  private Integer remainingServiceDays;
  private BigDecimal recommendedSafeRatio;
  private BigDecimal recommendedBalancedRatio;
  private BigDecimal recommendedAggressiveRatio;
  private String recommendReason;
  private LocalDateTime createdAt;
}
