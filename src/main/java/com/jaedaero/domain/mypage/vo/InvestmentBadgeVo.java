package com.jaedaero.domain.mypage.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class InvestmentBadgeVo {
  private String badgeCode;
  private LocalDateTime updatedAt;
}
