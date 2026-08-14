package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.mapper.MilitaryInfoMapper;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import java.time.Clock;
import java.time.LocalDate;
import java.time.YearMonth;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/** 입대일을 기준으로 soldier_profile의 현재 계급을 하루 한 번 동기화합니다. */
@Service
@RequiredArgsConstructor
public class MilitaryRankRefreshService {

  private final MilitaryInfoMapper militaryInfoMapper;
  private final Clock clock;

  public int refreshCurrentRanks() {
    YearMonth currentMonth = YearMonth.from(LocalDate.now(clock));
    int updated = 0;
    for (var profile : militaryInfoMapper.findProfilesForRankRefresh()) {
      String expectedRank =
          DefaultMilitaryPayPolicy.rankName(
              profile.getSoldierType(),
              java.time.temporal.ChronoUnit.MONTHS.between(
                  YearMonth.from(profile.getEnlistmentDate()), currentMonth));
      updated += militaryInfoMapper.updateRankName(profile.getUserId(), expectedRank);
    }
    return updated;
  }
}
