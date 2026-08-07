package com.jaedaero.domain.auth.service.impl;

import com.jaedaero.domain.auth.common.enums.AgreementType;
import com.jaedaero.domain.auth.dto.UserAgreementRequest;
import com.jaedaero.domain.auth.dto.UserAgreementResponse;
import com.jaedaero.domain.auth.exception.AgreementException;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.mapper.AuthUserMapper;
import com.jaedaero.domain.auth.service.AgreementService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class AgreementServiceImpl implements AgreementService {

  private static final String CURRENT_AGREEMENT_VERSION = "1.0";

  private final AuthUserMapper authUserMapper;

  /** 사용자 약관 동의 정보를 저장합니다. */
  @Override
  @Transactional
  public UserAgreementResponse recordAgreements(long userId, UserAgreementRequest request) {
    validateRequiredAgreements(request);
    if (authUserMapper.countActiveByUserId(userId) == 0) {
      throw new AgreementException(AuthErrorCode.USER_NOT_FOUND, "사용자를 찾을 수 없습니다.");
    }
    insertAgreement(userId, AgreementType.SERVICE_USE, true);
    insertAgreement(userId, AgreementType.PERSONAL_INFORMATION_COLLECTION, true);
    insertAgreement(userId, AgreementType.FINANCIAL_INFORMATION_INQUIRY, true);
    insertAgreement(userId, AgreementType.AI_SERVICE_USE, true);
    insertAgreement(userId, AgreementType.MARKETING_INFORMATION_RECEIPT, request.getMarketingInformationReceiptAgreed());

    return new UserAgreementResponse(
        true,
        true,
        true,
        true,
        request.getMarketingInformationReceiptAgreed());
  }

  /** 필수 약관 동의 여부를 검증합니다. */
  private void validateRequiredAgreements(UserAgreementRequest request) {
    if (!Boolean.TRUE.equals(request.getServiceUseAgreed())
        || !Boolean.TRUE.equals(request.getPersonalInformationCollectionAgreed())
        || !Boolean.TRUE.equals(request.getFinancialInformationInquiryAgreed())
        || !Boolean.TRUE.equals(request.getAiServiceUseAgreed())) {
      throw new AgreementException(
          AuthErrorCode.REQUIRED_AGREEMENT_NOT_ACCEPTED, "필수 약관에 모두 동의해야 합니다.");
    }
  }

  /** 약관 유형별 동의 이력을 저장합니다. */
  private void insertAgreement(long userId, AgreementType agreementType, boolean required) {
    authUserMapper.insertAgreement(
        userId, agreementType.name(), CURRENT_AGREEMENT_VERSION, required);
  }
}
