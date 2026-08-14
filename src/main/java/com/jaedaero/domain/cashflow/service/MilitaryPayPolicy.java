package com.jaedaero.domain.cashflow.service;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import java.time.YearMonth;

public interface MilitaryPayPolicy {
  DefaultMilitaryPayPolicy.MilitaryPay resolve(
      SoldierType soldierType, YearMonth enlistmentMonth, YearMonth month);
}
