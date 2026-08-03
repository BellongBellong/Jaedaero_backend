package com.jaedaero.domain.strategyapplication.service;

import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import java.util.List;

public interface StrategyApplicationService {

  StrategyApplicationResponse applyAiRecommendation(long userId, long analysisId);

  List<StrategyApplicationResponse> getHistory(long userId, int page, int size);
}
