package com.jaedaero.domain.aianalysis.service;

import com.jaedaero.domain.aianalysis.dto.SpendingExpectedEffectResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingImprovementResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingInsightResponse;
import com.jaedaero.domain.aianalysis.dto.SpendingPatternResponse;
import java.util.List;

public record SpendingAnalysis(
    SpendingPatternResponse pattern,
    List<SpendingInsightResponse> insights,
    SpendingImprovementResponse improvement,
    SpendingExpectedEffectResponse expectedEffect) {}
