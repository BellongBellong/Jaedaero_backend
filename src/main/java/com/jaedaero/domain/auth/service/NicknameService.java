package com.jaedaero.domain.auth.service;

public interface NicknameService {

  /** 현재 사용자를 제외하고 닉네임 사용 가능 여부를 확인합니다. */
  boolean isAvailable(long userId, String nickname);

  /** 사용자의 닉네임을 변경합니다. */
  void updateNickname(long userId, String nickname);
}
