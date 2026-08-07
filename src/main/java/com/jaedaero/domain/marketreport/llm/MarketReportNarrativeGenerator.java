package com.jaedaero.domain.marketreport.llm;

import com.jaedaero.domain.aianalysis.llm.OpenAiModel;

public interface MarketReportNarrativeGenerator {
  MarketReportNarrative generate(OpenAiModel model, String prompt);
}
