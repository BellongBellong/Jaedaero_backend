package com.jaedaero.domain.aianalysis.vo;

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
public class AiAnalysisVo {
  private Long analysisId;
  private Long userId;
  private Long snapshotId;
  private Long simulationId;
  private AiAnalysisType analysisType;
  private String resultJson;
  private String inputDataHash;
  private String modelName;
  private String promptVersion;
  private LocalDateTime createdAt;
}
