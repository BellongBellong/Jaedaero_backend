package com.jaedaero.domain.auth.service;

import com.jaedaero.domain.auth.dto.UserAgreementRequest;
import com.jaedaero.domain.auth.dto.UserAgreementResponse;

public interface AgreementService {

  /** 사용자의 약관 동의 내역을 기록합니다. */
  UserAgreementResponse recordAgreements(long userId, UserAgreementRequest request);
}
