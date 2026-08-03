package com.jaedaero.domain.aianalysis.dto;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
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
public class AiAnalysisResult {
  private LocalDate financialDischargeDate;
  private Integer deltaDaysVsActual;
  private BigDecimal achievementRate;
  private String comment;
  private AiRecommendedScenarioResponse recommendedScenario;
  private LocalDate baselineDischargeDate;
  private Integer deltaDaysVsBaseline;
  private List<String> pros;
  private List<String> cons;
}
