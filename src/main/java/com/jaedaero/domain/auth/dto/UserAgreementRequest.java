package com.jaedaero.domain.auth.dto;

import javax.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class UserAgreementRequest {

  @NotNull private Boolean serviceUseAgreed;

  @NotNull private Boolean personalInformationCollectionAgreed;

  @NotNull private Boolean financialInformationInquiryAgreed;

  @NotNull private Boolean aiServiceUseAgreed;
}
