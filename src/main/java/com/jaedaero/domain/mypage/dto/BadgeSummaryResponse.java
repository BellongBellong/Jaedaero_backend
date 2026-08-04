package com.jaedaero.domain.mypage.dto;

import java.util.List;
import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class BadgeSummaryResponse {

  private final int earnedCount;
  private final List<String> recentBadges;
}
