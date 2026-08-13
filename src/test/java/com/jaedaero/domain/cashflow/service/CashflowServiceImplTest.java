package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.CashflowMapper;
import com.jaedaero.domain.cashflow.mapper.MilitaryPayPolicyMapper;
import com.jaedaero.domain.cashflow.service.impl.CashflowServiceImpl;
import com.jaedaero.domain.cashflow.vo.CashflowForecastMonthVo;
import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import com.jaedaero.domain.cashflow.vo.CashflowInputSourceVo;
import com.jaedaero.domain.cashflow.vo.SoldierSavingInputSourceVo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

class CashflowServiceImplTest {

  @Test
  void generatePersistsSummaryAndMonthlyRows_thenLatestReturnsThem() {
    InMemoryCashflowMapper mapper = new InMemoryCashflowMapper();
    CashflowService service = service(mapper);

    CashflowForecastResponse generated = service.generate(1L);
    CashflowForecastResponse latest = service.getLatest(1L);

    assertEquals(1L, generated.getForecastId());
    assertEquals(18, generated.getMonths().size());
    assertEquals(10_200_000L, generated.getExpectedSalary());
    assertEquals(8_000_000L, generated.getExpectedSavingAmount());
    assertEquals(2_200_000L, generated.getExpectedInvestmentAmount());
    assertEquals(10_200_000L, generated.getExpectedAsset());
    assertEquals(244_776L, generated.getSoldierSavingInterest());
    assertEquals(8_000_000L, generated.getGovernmentMatchingSupport());
    assertEquals(0L, generated.getExpectedInvestmentReturn());
    assertEquals(18_444_776L, generated.getPotentialExpectedAsset());
    assertEquals(false, generated.getReturnsIncludedInExpectedAsset());
    assertEquals(
        ConservativeMonthlyCashflowEngine.CALCULATION_POLICY_VERSION,
        generated.getCalculationPolicyVersion());
    assertEquals(0L, generated.getMonths().get(0).getExpectedInvestmentAmount());
    assertEquals(1, mapper.forecasts.size());
    assertEquals(18, mapper.months.size());
    assertEquals(generated.getForecastId(), latest.getForecastId());
    assertEquals(generated.getExpectedAsset(), latest.getExpectedAsset());
    assertEquals(generated.getPotentialExpectedAsset(), latest.getPotentialExpectedAsset());
  }

  @Test
  void latestWithoutGeneratedForecastIsRejected() {
    assertThrows(CashflowException.class, () -> service(new InMemoryCashflowMapper()).getLatest(1L));
  }

  @Test
  void latestLimitsMonthlyRowsToRequestedMonths() {
    InMemoryCashflowMapper mapper = new InMemoryCashflowMapper();
    CashflowService service = service(mapper);
    service.generate(1L);

    CashflowForecastResponse latest = service.getLatest(1L, 2);

    assertEquals(2, latest.getMonths().size());
  }

  @Test
  void calculationInputUsesTheSameCanonicalInputAsForecastGeneration() {
    CashflowService service = service(new InMemoryCashflowMapper());

    var input = service.getCalculationInput(1L);

    assertEquals(30_000_000L, input.getTargetAmount());
    assertEquals(SoldierType.ARMY, input.getSoldierType());
    assertEquals(LocalDate.of(2027, 6, 1), input.getDischargeDate());
  }

  private CashflowService service(InMemoryCashflowMapper mapper) {
    CashflowInputProvider inputProvider =
        userId ->
            new CashflowInput(
                0L,
                30_000_000L,
                0L,
                SoldierType.ARMY,
                LocalDate.of(2026, 1, 1),
                LocalDate.of(2027, 6, 1));
    return new CashflowServiceImpl(
        mapper,
        inputProvider,
        new CashflowCalculator(policy()),
        Clock.fixed(Instant.parse("2026-01-10T00:00:00Z"), ZoneId.of("Asia/Seoul")));
  }

  private DefaultMilitaryPayPolicy policy() {
    Map<String, Long> salaries =
        Map.of("이병", 200_000L, "일병", 350_000L, "상병", 650_000L, "병장", 950_000L);
    MilitaryPayPolicyMapper mapper =
        (soldierType, rankName, monthStart, monthEnd) -> salaries.get(rankName);
    return new DefaultMilitaryPayPolicy(mapper);
  }

  private static class InMemoryCashflowMapper implements CashflowMapper {
    private final List<CashflowForecastVo> forecasts = new ArrayList<>();
    private final List<CashflowForecastMonthVo> months = new ArrayList<>();

    @Override
    public CashflowInputSourceVo findInputByUserId(long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<SoldierSavingInputSourceVo> findSoldierSavingsByUserId(long userId) {
      return List.of();
    }

    @Override
    public void insertForecast(CashflowForecastVo forecast) {
      forecast.setForecastId((long) forecasts.size() + 1);
      forecasts.add(forecast);
    }

    @Override
    public void insertForecastMonths(long forecastId, List<CashflowForecastMonthVo> values) {
      values.forEach(month -> month.setForecastId(forecastId));
      months.addAll(values);
    }

    @Override
    public CashflowForecastVo findLatestForecastByUserId(long userId) {
      return forecasts.isEmpty() ? null : forecasts.get(forecasts.size() - 1);
    }

    @Override
    public List<CashflowForecastMonthVo> findMonthsByForecastId(long forecastId) {
      return months.stream().filter(month -> forecastId == month.getForecastId()).toList();
    }

    @Override
    public LocalDate findDischargeDateByUserId(long userId) {
      return LocalDate.of(2027, 6, 1);
    }
  }
}
