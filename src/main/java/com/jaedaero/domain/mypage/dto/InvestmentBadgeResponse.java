package com.jaedaero.domain.mypage.dto;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class InvestmentBadgeResponse {
  private final String badgeCode;
  private final LocalDateTime acquiredAt;
}
