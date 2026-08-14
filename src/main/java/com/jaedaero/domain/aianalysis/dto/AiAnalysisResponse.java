package com.jaedaero.domain.aianalysis.dto;

import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AiAnalysisResponse {
  private final Long analysisId;
  private final Long simulationId;
  private final Long snapshotId;
  private final AiAnalysisType analysisType;
  private final AiGenerationSource generationSource;
  private final Long expectedAsset;
  private final LocalDate financialDischargeDate;
  private final Integer deltaDaysVsActual;
  private final BigDecimal achievementRate;
  private final String comment;
  private final AiRecommendedScenarioResponse recommendedScenario;
  private final LocalDate baselineDischargeDate;
  private final Integer deltaDaysVsBaseline;
  private final List<String> pros;
  private final List<String> cons;
  private final SpendingPatternResponse spendingPattern;
  private final List<SpendingInsightResponse> spendingInsights;
  private final SpendingImprovementResponse spendingImprovement;
  private final SpendingExpectedEffectResponse spendingExpectedEffect;
}
