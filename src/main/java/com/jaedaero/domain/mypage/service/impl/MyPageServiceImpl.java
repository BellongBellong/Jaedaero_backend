package com.jaedaero.domain.mypage.service.impl;

import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.mypage.dto.BadgeSummaryResponse;
import com.jaedaero.domain.mypage.dto.InvestmentBadgeResponse;
import com.jaedaero.domain.mypage.dto.InvestmentBadgeStatusResponse;
import com.jaedaero.domain.mypage.dto.MyPageProfileResponse;
import com.jaedaero.domain.mypage.exception.MyPageErrorCode;
import com.jaedaero.domain.mypage.exception.MyPageException;
import com.jaedaero.domain.mypage.mapper.MyPageMapper;
import com.jaedaero.domain.mypage.service.MyPageService;
import com.jaedaero.domain.mypage.vo.MyPageProfileVo;
import com.jaedaero.domain.mypage.vo.InvestmentBadgeStatusVo;
import java.util.List;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

  private final MyPageMapper myPageMapper;
  private final AuthUserMapper authUserMapper;

  /** 사용자의 마이페이지 프로필과 뱃지 현황을 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public MyPageProfileResponse getProfile(long userId) {
    MyPageProfileVo profile = myPageMapper.findActiveProfile(userId);
    if (profile == null) {
      throw new MyPageException(MyPageErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }
    return new MyPageProfileResponse(
        profile.getNickname(),
        profile.getProfileImage(),
        profile.getProfileSource(),
        profile.getSoldierType(),
        profile.getMilitaryRank(),
        new BadgeSummaryResponse(
            myPageMapper.countEarnedBadges(userId), myPageMapper.findRecentBadgeCodes(userId)),
        toInvestmentBadgeStatus(myPageMapper.findInvestmentBadgeStatus(userId)));
  }

  /** 뱃지 상태 VO를 마이페이지 응답 DTO로 변환합니다. */
  private InvestmentBadgeStatusResponse toInvestmentBadgeStatus(InvestmentBadgeStatusVo badgeStatus) {
    if (badgeStatus == null) {
      return new InvestmentBadgeStatusResponse(null, null, null, null, 0, 0, 0);
    }
    return new InvestmentBadgeStatusResponse(
        badgeStatus.getInitialPreference(),
        badgeStatus.getBadgeTier(),
        badgeStatus.getSafeGrade(),
        badgeStatus.getAggressiveGrade(),
        badgeStatus.getSafeMissionCount(),
        badgeStatus.getAggressiveMissionCount(),
        badgeStatus.getSafeMissionCount() + badgeStatus.getAggressiveMissionCount());
  }

  /** 사용자가 획득한 투자 뱃지를 페이지 단위로 조회합니다. */
  @Override
  @Transactional(readOnly = true)
  public List<InvestmentBadgeResponse> getInvestmentBadges(long userId) {
    return myPageMapper.findInvestmentBadges(userId).stream()
        .map(
            badge ->
                new InvestmentBadgeResponse(
                    badge.getBadgeName(),
                    badge.getBadgeDescription(),
                    badge.getMissionType(),
                    badge.getGrade(),
                    badge.getRequiredMissionCount(),
                    badge.getMissionCompletedCount(),
                    badge.isAchieved(),
                    badge.getAcquiredAt()))
        .collect(Collectors.toList());
  }

  /** 사용자를 탈퇴 처리하고 인증 토큰을 삭제합니다. */
  @Override
  @Transactional
  public void unlinkCodef(long userId) {
    if (myPageMapper.disconnectCodefConnection(userId) == 0) {
      throw new MyPageException(
          MyPageErrorCode.CODEF_CONNECTION_NOT_FOUND, "활성 금융기관 연동 정보를 찾을 수 없습니다.");
    }
    myPageMapper.disconnectCodefInstitutionConnections(userId);
    myPageMapper.disconnectConnectedAccounts(userId);
  }

  @Override
  @Transactional
  public void withdraw(long userId) {
    if (myPageMapper.withdraw(userId) == 0) {
      throw new MyPageException(MyPageErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }
    authUserMapper.deleteRefreshTokensByUserId(userId);
  }
}
