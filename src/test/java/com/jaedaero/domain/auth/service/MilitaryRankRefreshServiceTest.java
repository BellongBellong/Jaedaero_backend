package com.jaedaero.domain.auth.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.auth.mapper.MilitaryInfoMapper;
import com.jaedaero.domain.auth.vo.SoldierProfileVo;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class MilitaryRankRefreshServiceTest {

  @Test
  void refreshesRankFromEnlistmentMonth() {
    SoldierProfileVo profile = new SoldierProfileVo();
    profile.setUserId(1L);
    profile.setSoldierType(SoldierType.ARMY);
    profile.setRankName("이병");
    profile.setEnlistmentDate(LocalDate.of(2026, 6, 1));
    profile.setDischargeDate(LocalDate.of(2027, 12, 15));
    RecordingMilitaryInfoMapper mapper = new RecordingMilitaryInfoMapper(profile);
    MilitaryRankRefreshService service =
        new MilitaryRankRefreshService(
            mapper,
            Clock.fixed(Instant.parse("2026-08-14T00:00:00Z"), ZoneId.of("Asia/Seoul")));

    assertEquals(1, service.refreshCurrentRanks());
    assertEquals("일병", mapper.updatedRankName);
  }

  private static class RecordingMilitaryInfoMapper implements MilitaryInfoMapper {
    private final List<SoldierProfileVo> profiles;
    private String updatedRankName;

    private RecordingMilitaryInfoMapper(SoldierProfileVo profile) {
      this.profiles = new ArrayList<>(List.of(profile));
    }

    @Override
    public int countActiveUserByUserId(long userId) {
      return 1;
    }

    @Override
    public int countSoldierSavingByUserId(long userId) {
      return 0;
    }

    @Override
    public void upsertSoldierProfile(SoldierProfileVo soldierProfile) {}

    @Override
    public List<SoldierProfileVo> findProfilesForRankRefresh() {
      return profiles;
    }

    @Override
    public int updateRankName(long userId, String rankName) {
      updatedRankName = rankName;
      return 1;
    }

    @Override
    public void insertChallengeGroupIgnore(String soldierType, int enlistmentYear, int enlistmentMonth) {}

    @Override
    public Long findChallengeGroupId(String soldierType, int enlistmentYear, int enlistmentMonth) {
      return null;
    }

    @Override
    public void insertChallengeMemberIgnore(long groupId, long userId) {}

    @Override
    public BigDecimal findChallengeGroupTargetAmountAverage(long groupId) {
      return null;
    }
  }
}
