package com.jaedaero.domain.mypage.service.impl;

import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.mypage.dto.BadgeSummaryResponse;
import com.jaedaero.domain.mypage.dto.MyPageProfileResponse;
import com.jaedaero.domain.mypage.exception.MyPageErrorCode;
import com.jaedaero.domain.mypage.exception.MyPageException;
import com.jaedaero.domain.mypage.mapper.MyPageMapper;
import com.jaedaero.domain.mypage.service.MyPageService;
import com.jaedaero.domain.mypage.vo.MyPageProfileVo;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class MyPageServiceImpl implements MyPageService {

  private final MyPageMapper myPageMapper;
  private final AuthUserMapper authUserMapper;

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
        profile.getMilitaryRank(),
        new BadgeSummaryResponse(
            myPageMapper.countEarnedBadges(userId), myPageMapper.findRecentBadgeCodes(userId)));
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
