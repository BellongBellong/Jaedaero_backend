package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.NicknameException;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.service.NicknameService;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class NicknameServiceImpl implements NicknameService {

  private final AuthUserMapper authUserMapper;

  /** 현재 사용자가 사용할 수 있는 닉네임인지 확인합니다. */
  @Override
  @Transactional(readOnly = true)
  public boolean isAvailable(long userId, String nickname) {
    return authUserMapper.availabilityNickname(normalize(nickname), userId) == 0;
  }

  /** 사용자의 닉네임을 변경합니다. */
  @Override
  @Transactional
  public void updateNickname(long userId, String nickname) {
    String normalizedNickname = normalize(nickname);
    if (authUserMapper.availabilityNickname(normalizedNickname, userId) > 0) {
      throw new NicknameException(
          AuthErrorCode.NICKNAME_ALREADY_IN_USE, "이미 사용 중인 닉네임입니다.");
    }

    try {
      if (authUserMapper.updateNickname(userId, normalizedNickname) == 0) {
        throw new NicknameException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
      }
    } catch (DuplicateKeyException exception) {
      throw new NicknameException(
          AuthErrorCode.NICKNAME_ALREADY_IN_USE, "이미 사용 중인 닉네임입니다.");
    }
  }

  /** 닉네임을 정규화하고 형식을 검증합니다. */
  private String normalize(String nickname) {
    if (nickname == null) {
      throw new NicknameException(
          AuthErrorCode.INVALID_NICKNAME, "닉네임은 공백일 수 없습니다.");
    }

    String normalizedNickname = nickname.trim();
    if (!normalizedNickname.matches("[가-힣a-zA-Z]{2,12}")) {
      throw new NicknameException(
          AuthErrorCode.INVALID_NICKNAME, "닉네임은 한글 또는 영문 1자 이상 12자 이하여야 합니다.");
    }
    return normalizedNickname;
  }
}
