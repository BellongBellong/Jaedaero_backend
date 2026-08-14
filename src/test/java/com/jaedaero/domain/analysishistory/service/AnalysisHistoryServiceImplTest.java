package com.jaedaero.domain.analysishistory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.jaedaero.domain.aianalysis.dto.AiGenerationSource;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisType;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryFilter;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryPageResponse;
import com.jaedaero.domain.analysishistory.dto.AnalysisHistoryType;
import com.jaedaero.domain.analysishistory.mapper.AnalysisHistoryMapper;
import com.jaedaero.domain.analysishistory.service.impl.AnalysisHistoryServiceImpl;
import com.jaedaero.domain.analysishistory.vo.AnalysisHistoryRow;
import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.service.DefaultMilitaryPayPolicy;
import com.jaedaero.domain.simulation.service.SimulationAllocationPolicy;
import com.jaedaero.domain.simulation.service.SimulationCalculator;
import com.jaedaero.domain.simulation.service.SimulationInput;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import org.junit.jupiter.api.Test;

class AnalysisHistoryServiceImplTest {

  @Test
  void allHistoriesContainTabCardFieldsSummaryAndAppliedState() {
    InMemoryAnalysisHistoryMapper mapper =
        new InMemoryAnalysisHistoryMapper(List.of(whatIfRow(), aiRow()));
    AnalysisHistoryService service = service(mapper);

    AnalysisHistoryPageResponse response =
        service.getHistories(1L, AnalysisHistoryFilter.ALL, 0, 20);

    assertEquals(2L, response.getTotalCount());
    assertEquals(2, response.getHistories().size());
    assertFalse(response.getHasNext());
    assertEquals(LocalDateTime.of(2026, 8, 6, 11, 0), response.getLatestAnalyzedAt());
    assertEquals(18_750_000L, response.getLatestExpectedAsset());

    var ai = response.getHistories().get(0);
    assertEquals(AnalysisHistoryType.AI, ai.getHistoryType());
    assertEquals(21L, ai.getSourceId());
    assertEquals("AI 소비 분석", ai.getTitle());
    assertEquals("소비가 줄었고 자산 흐름이 개선됐습니다.", ai.getSummary());
    assertEquals(195_800L, ai.getSpendingAmount());
    assertEquals(-20_000L, ai.getSpendingChangeAmount());
    assertEquals(new BigDecimal("-9.27"), ai.getSpendingChangeRate());
    assertEquals(1_480_000L, ai.getExpectedAssetIncreaseAmount());
    assertEquals(AiGenerationSource.OPENAI, ai.getGenerationSource());
    assertEquals(18_750_000L, ai.getExpectedAsset());
    assertFalse(ai.getIsApplied());
    assertNull(ai.getMonthlyInvestmentAmount());

    var whatIf = response.getHistories().get(1);
    assertEquals(AnalysisHistoryType.WHAT_IF, whatIf.getHistoryType());
    assertEquals(11L, whatIf.getSourceId());
    assertEquals("내 집 마련 계획", whatIf.getTitle());
    assertEquals("월 소비 180,000원 · 군적금 300,000원 · 투자 150,000원 계획", whatIf.getSummary());
    assertEquals(18_540_000L, whatIf.getExpectedAsset());
    assertTrue(whatIf.getIsApplied());
    assertEquals(new BigDecimal("20.00"), whatIf.getSpendingRate());
    assertEquals(new BigDecimal("33.33"), whatIf.getSavingRate());
    assertEquals(new BigDecimal("16.67"), whatIf.getInvestmentRate());
    assertNull(whatIf.getGenerationSource());
  }

  @Test
  void typeFilterPaginationAndEmptyPageUseConsistentMetadata() {
    InMemoryAnalysisHistoryMapper mapper =
        new InMemoryAnalysisHistoryMapper(List.of(whatIfRow(), aiRow()));
    AnalysisHistoryService service = service(mapper);

    AnalysisHistoryPageResponse first =
        service.getHistories(1L, AnalysisHistoryFilter.ALL, 0, 1);
    AnalysisHistoryPageResponse aiOnly =
        service.getHistories(1L, AnalysisHistoryFilter.AI, 0, 20);
    AnalysisHistoryPageResponse empty =
        service.getHistories(1L, AnalysisHistoryFilter.WHAT_IF, 1, 20);

    assertTrue(first.getHasNext());
    assertEquals(AnalysisHistoryType.AI, first.getHistories().get(0).getHistoryType());
    assertEquals(1L, aiOnly.getTotalCount());
    assertEquals(AnalysisHistoryType.AI, aiOnly.getHistories().get(0).getHistoryType());
    assertTrue(empty.getHistories().isEmpty());
    assertEquals(1L, empty.getTotalCount());
    assertFalse(empty.getHasNext());
    assertEquals(18_540_000L, empty.getLatestExpectedAsset());
  }

  @Test
  void unreadableLegacyAiResultFallsBackWithoutFailingThePage() {
    AnalysisHistoryRow row = aiRow();
    row.setResultJson("not-json");
    AnalysisHistoryService service =
        service(new InMemoryAnalysisHistoryMapper(List.of(row)));

    AnalysisHistoryPageResponse response =
        service.getHistories(1L, AnalysisHistoryFilter.AI, 0, 20);

    assertEquals(1, response.getHistories().size());
    assertEquals(
        "거래 및 자산 흐름을 바탕으로 생성한 AI 분석입니다.",
        response.getHistories().get(0).getSummary());
    assertNull(response.getHistories().get(0).getExpectedAsset());
    assertNull(response.getLatestExpectedAsset());
  }

  private AnalysisHistoryService service(AnalysisHistoryMapper mapper) {
    SimulationCalculator calculator =
        new SimulationCalculator(
            new DefaultMilitaryPayPolicy((soldierType, rankName, from, to) -> 900_000L));
    return new AnalysisHistoryServiceImpl(
        mapper,
        userId ->
            new SimulationInput(
                4_300_000L,
                20_000_000L,
                400_000L,
                SoldierType.ARMY,
                LocalDate.of(2026, 3, 1),
                LocalDate.of(2027, 9, 1)),
        calculator,
        new SimulationAllocationPolicy(),
        new ObjectMapper());
  }

  private AnalysisHistoryRow whatIfRow() {
    return AnalysisHistoryRow.builder()
        .historyType(AnalysisHistoryType.WHAT_IF)
        .sourceId(11L)
        .scenarioName("내 집 마련 계획")
        .createdAt(LocalDateTime.of(2026, 8, 6, 10, 0))
        .expectedAsset(18_540_000L)
        .monthlySpendingAmount(180_000L)
        .monthlySavingAmount(300_000L)
        .monthlyInvestmentAmount(150_000L)
        .applied(true)
        .build();
  }

  private AnalysisHistoryRow aiRow() {
    return AnalysisHistoryRow.builder()
        .historyType(AnalysisHistoryType.AI)
        .sourceId(21L)
        .analysisType(AiAnalysisType.CONSUMPTION)
        .createdAt(LocalDateTime.of(2026, 8, 6, 11, 0))
        .generationSource(AiGenerationSource.OPENAI)
        .resultJson(
            """
            {
              "comment": "소비가 줄었고\\n자산 흐름이 개선됐습니다.",
              "expectedAsset": 18750000,
              "recommendedScenario": {"expectedAsset": 19900000},
              "spendingPattern": {
                "totalSpendingAmount": 195800,
                "changeAmount": -20000,
                "changeRate": -9.27
              },
              "spendingExpectedEffect": {"expectedAssetIncreaseAmount": 1480000}
            }
            """)
        .applied(false)
        .build();
  }

  private static class InMemoryAnalysisHistoryMapper implements AnalysisHistoryMapper {

    private final List<AnalysisHistoryRow> rows;

    private InMemoryAnalysisHistoryMapper(List<AnalysisHistoryRow> rows) {
      this.rows = new ArrayList<>(rows);
      this.rows.sort(
          Comparator.comparing(AnalysisHistoryRow::getCreatedAt)
              .reversed()
              .thenComparing(AnalysisHistoryRow::getHistoryType)
              .thenComparing(AnalysisHistoryRow::getSourceId, Comparator.reverseOrder()));
    }

    @Override
    public List<AnalysisHistoryRow> findByUserId(
        long userId, String type, long offset, int limit) {
      List<AnalysisHistoryRow> filtered = filtered(type);
      int from = (int) Math.min(offset, filtered.size());
      int to = Math.min(from + limit, filtered.size());
      return filtered.subList(from, to);
    }

    @Override
    public long countByUserId(long userId, String type) {
      return filtered(type).size();
    }

    @Override
    public AnalysisHistoryRow findLatestByUserId(long userId, String type) {
      return filtered(type).stream().findFirst().orElse(null);
    }

    private List<AnalysisHistoryRow> filtered(String type) {
      return rows.stream()
          .filter(row -> "ALL".equals(type) || row.getHistoryType().name().equals(type))
          .toList();
    }
  }
}
