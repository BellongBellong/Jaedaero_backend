package com.jaedaero.domain.aianalysis.service;

import com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest;
import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;

public interface AiAnalysisService {
  AiAnalysisResponse analyze(long userId, AiAnalysisRequest request);
  AiAnalysisResponse getDetail(long userId, long analysisId);
}
