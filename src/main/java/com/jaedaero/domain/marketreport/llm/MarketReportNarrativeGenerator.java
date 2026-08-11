package com.jaedaero.domain.marketreport.llm;

import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import java.util.List;

public interface MarketReportNarrativeGenerator {
  MarketReportNarrative generate(String prompt);

  default MarketReportNarrative generate(
      String prompt, List<MarketReportSourceItem> availableSources) {
    return generate(prompt);
  }
}
