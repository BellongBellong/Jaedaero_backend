package com.jaedaero.domain.marketreport.llm;

import com.jaedaero.domain.marketreport.dto.MarketReportSourceItem;
import java.util.List;

/** Gemini가 생성한 구조화된 사실 기반 시장 리포트와 인용 출처. */
public record MarketReportNarrative(
    String title, String summary, String content, List<MarketReportSourceItem> sources) {

  public static final int MAX_TITLE_CHAR_COUNT = 200;
  public static final int MAX_SUMMARY_CHAR_COUNT = 500;

  public MarketReportNarrative {
    title = normalizeLine(title);
    summary = normalizeLine(summary);
    content = content == null ? "" : content.trim();
    sources = sources == null ? List.of() : List.copyOf(sources);
  }

  private static String normalizeLine(String value) {
    return value == null ? "" : value.replaceAll("\\R+", " ").trim();
  }
}
