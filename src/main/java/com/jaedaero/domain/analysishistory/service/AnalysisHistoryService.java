package com.jaedaero.domain.analysishistory.service;

import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryFilter;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryPageResponse;

public interface AnalysisHistoryService {

  AnalysisHistoryPageResponse getHistories(
      long userId, AnalysisHistoryFilter type, int page, int size);
}
