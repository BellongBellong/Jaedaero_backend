package com.jaedaero.domain.auth.service;

public interface NicknameService {

  boolean isAvailable(long userId, String nickname);

  void updateNickname(long userId, String nickname);
}
