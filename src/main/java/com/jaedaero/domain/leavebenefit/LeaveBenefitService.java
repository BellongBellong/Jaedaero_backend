package com.jaedaero.domain.leavebenefit;

import java.util.List;

public interface LeaveBenefitService {

  List<LeaveBenefitResponse> getAvailableBenefits(LeaveBenefitCategory category);
}
