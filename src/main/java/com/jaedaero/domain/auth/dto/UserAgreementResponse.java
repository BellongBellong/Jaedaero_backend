package com.jaedaero.domain.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class UserAgreementResponse {

  private final boolean serviceUseAgreed;
  private final boolean personalInformationCollectionAgreed;
  private final boolean financialInformationInquiryAgreed;
  private final boolean aiServiceUseAgreed;
}
