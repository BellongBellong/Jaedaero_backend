package com.jaedaero.domain.investmentguidance.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
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
public class InvestmentGuidanceInputSourceVo {

  private Long forecastId;
  private Long baselineExpectedAsset;
  private Long brokerageBookValue;
  private LocalDateTime brokerageLastSyncedAt;
  private Long targetAmount;
  private String rankName;
  private LocalDate dischargeDate;
  private BigDecimal expectedReturnRate;
}
