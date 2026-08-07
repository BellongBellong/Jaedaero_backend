package com.jaedaero.domain.aianalysis.service;

import java.time.LocalDate;

public interface SpendingPatternProvider {
  SpendingPatternSnapshot load(long userId, LocalDate asOfDate);
}
