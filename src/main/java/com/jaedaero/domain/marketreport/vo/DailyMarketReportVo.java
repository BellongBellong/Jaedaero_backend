package com.jaedaero.domain.marketreport.vo;

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
public class DailyMarketReportVo {

  private Long reportId;
  private LocalDate reportDate;
  private String content;
  private LocalDateTime validFrom;
  private LocalDateTime validUntil;
  private LocalDateTime createdAt;
}
