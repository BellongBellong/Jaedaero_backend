package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.dto.MilitaryInfoRequest;
import com.jaedaero.domain.auth.dto.SoldierProfileResponse;

public interface MilitaryInfoService {

  /** 사용자의 군 복무 정보를 등록합니다. */
  SoldierProfileResponse registerMilitaryInfo(long userId, MilitaryInfoRequest request);
}
