package com.jaedaero.domain.leavebenefit;

import java.time.Clock;
import java.time.LocalDate;
import java.util.List;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class LeaveBenefitServiceImpl implements LeaveBenefitService {

  private final LeaveBenefitMapper leaveBenefitMapper;
  private final Clock applicationClock;

  public LeaveBenefitServiceImpl(LeaveBenefitMapper leaveBenefitMapper, Clock applicationClock) {
    this.leaveBenefitMapper = leaveBenefitMapper;
    this.applicationClock = applicationClock;
  }

  @Override
  @Transactional(readOnly = true)
  public List<LeaveBenefitResponse> getAvailableBenefits(LeaveBenefitCategory category) {
    return leaveBenefitMapper.findAvailableBenefits(category, LocalDate.now(applicationClock));
  }
}
