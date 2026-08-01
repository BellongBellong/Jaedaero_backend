package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.dto.InvestmentPreferencePreviewResponse;
import com.jaedaero.domain.auth.dto.InvestmentPreferenceRequest;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.InvestmentPreferenceException;
import com.jaedaero.domain.auth.mapper.InvestmentPreferenceMapper;
import com.jaedaero.domain.auth.service.InvestmentPreferenceService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class InvestmentPreferenceServiceImpl implements InvestmentPreferenceService {

  private final InvestmentPreferenceMapper investmentPreferenceMapper;

  @Override
  @Transactional
  public InvestmentPreferencePreviewResponse registerInvestmentPreference(
      long userId, InvestmentPreferenceRequest request) {
    validateRequest(request);
    if (investmentPreferenceMapper.countActiveUserByUserId(userId) == 0) {
      throw new InvestmentPreferenceException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }

    investmentPreferenceMapper.upsertInitialPreference(userId, request.getInvestmentPreference());
    investmentPreferenceMapper.upsertGoalTargetAmount(userId, request.getTargetAmount());
    return new InvestmentPreferencePreviewResponse(
        true, request.getInvestmentPreference(), request.getTargetAmount());
  }

  private void validateRequest(InvestmentPreferenceRequest request) {
    if (request == null
        || request.getInvestmentPreference() == null
        || request.getTargetAmount() == null
        || request.getTargetAmount() < 0) {
      throw new InvestmentPreferenceException(
          AuthErrorCode.INVALID_TARGET_AMOUNT, "투자 성향과 목표 금액을 올바르게 입력해야 합니다.");
    }
  }
}
