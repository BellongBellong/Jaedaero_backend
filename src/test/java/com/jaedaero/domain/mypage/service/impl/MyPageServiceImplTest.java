package com.jaedaero.domain.mypage.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.auth.common.enums.ProfileImage;
import com.jaedaero.domain.auth.common.enums.ProfileSource;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.vo.AuthUserVo;
import com.jaedaero.domain.mypage.dto.MyPageProfileResponse;
import com.jaedaero.domain.mypage.exception.MyPageException;
import com.jaedaero.domain.mypage.mapper.MyPageMapper;
import com.jaedaero.domain.mypage.vo.MyPageProfileVo;
import com.jaedaero.domain.mypage.vo.InvestmentBadgeVo;
import com.jaedaero.domain.mypage.vo.InvestmentBadgeStatusVo;
import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.Test;

class MyPageServiceImplTest {

  @Test
  void returnsProfileAndBadgeSummary() {
    StubMyPageMapper myPageMapper = new StubMyPageMapper();
    MyPageProfileVo profile = new MyPageProfileVo();
    profile.setNickname("테스트사용자");
    profile.setProfileImage(ProfileImage.ARMY);
    profile.setProfileSource(ProfileSource.GREEN);
    profile.setSoldierType(SoldierType.ARMY);
    profile.setMilitaryRank("병장");
    myPageMapper.profile = profile;
    myPageMapper.badgeCount = 2;
    myPageMapper.badgeCodes = List.of("SAFE_GOLD", "AGGRESSIVE_SILVER");

    MyPageServiceImpl service = new MyPageServiceImpl(myPageMapper, new StubAuthUserMapper());

    MyPageProfileResponse response = service.getProfile(1L);

    assertEquals("테스트사용자", response.getNickname());
    assertEquals(2, response.getBadgeSummary().getEarnedCount());
    assertEquals(List.of("SAFE_GOLD", "AGGRESSIVE_SILVER"), response.getBadgeSummary().getRecentBadges());
  }

  @Test
  void throwsWhenProfileDoesNotExist() {
    MyPageServiceImpl service = new MyPageServiceImpl(new StubMyPageMapper(), new StubAuthUserMapper());

    assertThrows(MyPageException.class, () -> service.getProfile(1L));
  }

  @Test
  void returnsAllInvestmentBadgeProgressesWithoutPagination() {
    StubMyPageMapper myPageMapper = new StubMyPageMapper();
    InvestmentBadgeVo badge = new InvestmentBadgeVo();
    badge.setBadgeName("공격형 플래티넘");
    badge.setRequiredMissionCount(100);
    badge.setMissionCompletedCount(105);
    badge.setAchieved(true);
    badge.setAcquiredAt(LocalDateTime.of(2026, 8, 5, 9, 41));
    myPageMapper.investmentBadges = List.of(badge);
    MyPageServiceImpl service = new MyPageServiceImpl(myPageMapper, new StubAuthUserMapper());

    assertEquals("공격형 플래티넘", service.getInvestmentBadges(1L).get(0).getBadgeName());
    assertEquals(105, service.getInvestmentBadges(1L).get(0).getMissionCompletedCount());
    assertEquals(
        LocalDateTime.of(2026, 8, 5, 9, 41),
        service.getInvestmentBadges(1L).get(0).getAcquiredAt());
  }

  @Test
  void withdrawsUserAndDeletesRefreshTokens() {
    StubMyPageMapper myPageMapper = new StubMyPageMapper();
    StubAuthUserMapper authUserMapper = new StubAuthUserMapper();
    MyPageServiceImpl service = new MyPageServiceImpl(myPageMapper, authUserMapper);

    service.withdraw(1L);

    assertEquals(1L, myPageMapper.withdrawnUserId);
    assertEquals(1L, authUserMapper.refreshTokensDeletedForUserId);
  }

  @Test
  void disconnectsCodefConnectionAndRelatedData() {
    StubMyPageMapper myPageMapper = new StubMyPageMapper();
    MyPageServiceImpl service = new MyPageServiceImpl(myPageMapper, new StubAuthUserMapper());

    service.unlinkCodef(1L);

    assertEquals(1L, myPageMapper.disconnectedCodefConnectionForUserId);
    assertEquals(1L, myPageMapper.disconnectedInstitutionConnectionsForUserId);
    assertEquals(1L, myPageMapper.disconnectedAccountsForUserId);
  }

  @Test
  void throwsWhenActiveCodefConnectionDoesNotExist() {
    StubMyPageMapper myPageMapper = new StubMyPageMapper();
    myPageMapper.disconnectCodefConnectionCount = 0;
    MyPageServiceImpl service = new MyPageServiceImpl(myPageMapper, new StubAuthUserMapper());

    assertThrows(MyPageException.class, () -> service.unlinkCodef(1L));
    assertEquals(0L, myPageMapper.disconnectedInstitutionConnectionsForUserId);
    assertEquals(0L, myPageMapper.disconnectedAccountsForUserId);
  }

  private static class StubMyPageMapper implements MyPageMapper {
    private MyPageProfileVo profile;
    private int badgeCount;
    private List<String> badgeCodes = List.of();
    private List<InvestmentBadgeVo> investmentBadges = List.of();
    private long withdrawnUserId;
    private int disconnectCodefConnectionCount = 1;
    private long disconnectedCodefConnectionForUserId;
    private long disconnectedInstitutionConnectionsForUserId;
    private long disconnectedAccountsForUserId;

    @Override public MyPageProfileVo findActiveProfile(long userId) { return profile; }
    @Override public int countEarnedBadges(long userId) { return badgeCount; }
    @Override public List<String> findRecentBadgeCodes(long userId) { return badgeCodes; }
    @Override public List<InvestmentBadgeVo> findInvestmentBadges(long userId) { return investmentBadges; }
    @Override public InvestmentBadgeStatusVo findInvestmentBadgeStatus(long userId) { return null; }
    @Override public int disconnectCodefConnection(long userId) { disconnectedCodefConnectionForUserId = userId; return disconnectCodefConnectionCount; }
    @Override public void disconnectCodefInstitutionConnections(long userId) { disconnectedInstitutionConnectionsForUserId = userId; }
    @Override public void disconnectConnectedAccounts(long userId) { disconnectedAccountsForUserId = userId; }
    @Override public int withdraw(long userId) { withdrawnUserId = userId; return 1; }
  }

  private static class StubAuthUserMapper implements AuthUserMapper {
    private long refreshTokensDeletedForUserId;
    @Override public AuthUserVo findActiveBySocialIdentity(String socialType, String socialId) { return null; }
    @Override public int availabilityNickname(String nickname, long userId) { return 0; }
    @Override public int updateNickname(long userId, String nickname) { return 0; }
    @Override public int updateProfileAppearance(long userId, ProfileImage profileImage, ProfileSource profileSource) { return 0; }
    @Override public int countActiveByUserId(long userId) { return 0; }
    @Override public void insertAgreement(long userId, String agreementType, String agreementVersion, boolean required) {}
    @Override public void insert(String socialType, String socialId) {}
    @Override public AuthUserVo findActiveByRefreshTokenHash(String tokenHash) { return null; }
    @Override public void insertRefreshToken(long userId, String tokenHash, Timestamp expiresAt) {}
    @Override public void deleteRefreshTokenByHash(String tokenHash) {}
    @Override public void deleteRefreshTokensByUserId(long userId) { refreshTokensDeletedForUserId = userId; }
  }
}
