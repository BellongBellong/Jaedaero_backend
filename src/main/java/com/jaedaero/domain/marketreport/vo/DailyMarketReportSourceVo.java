package com.jaedaero.domain.marketreport.vo;

import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

/** 일일 시장 리포트의 인용 출처 저장 행. */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DailyMarketReportSourceVo {
  private Long sourceId;
  private Long reportId;
  private Integer sourceOrder;
  private String title;
  private String url;
  private LocalDateTime createdAt;
}
