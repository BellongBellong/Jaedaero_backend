package com.jaedaero.domain.analysishistory.vo;

import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryType;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AnalysisHistoryRow {

  private AnalysisHistoryType historyType;
  private Long sourceId;
  private String scenarioName;
  private AiAnalysisType analysisType;
  private LocalDateTime createdAt;
  private Long expectedAsset;
  private Long monthlySpendingAmount;
  private Long monthlySavingAmount;
  private Long monthlyInvestmentAmount;
  private AiGenerationSource generationSource;
  private String resultJson;
  private Boolean applied;
}
