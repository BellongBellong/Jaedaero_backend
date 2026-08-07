package com.jaedaero.domain.analysishistory.dto;

import java.time.LocalDateTime;
import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class AnalysisHistoryPageResponse {

  private final List<AnalysisHistoryItemResponse> histories;
  private final Integer page;
  private final Integer size;
  private final Long totalCount;
  private final Boolean hasNext;
  private final LocalDateTime latestAnalyzedAt;
  private final Long latestExpectedAsset;
}
