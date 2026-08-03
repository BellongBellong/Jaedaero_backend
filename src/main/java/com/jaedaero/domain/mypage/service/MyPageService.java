package com.jaedaero.domain.mypage.service;

import com.jaedaero.domain.mypage.dto.MyPageProfileResponse;

public interface MyPageService {

  /** 마이페이지 첫 화면에 필요한 프로필과 뱃지 정보를 조회합니다. */
  MyPageProfileResponse getProfile(long userId);

  /** 사용자 계정을 소프트 삭제하고 모든 리프레시 토큰을 무효화합니다. */
  void withdraw(long userId);
}
