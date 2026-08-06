package com.jaedaero.domain.cashflow.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.mapper.CashflowMapper;
import com.jaedaero.domain.cashflow.vo.CashflowForecastMonthVo;
import com.jaedaero.domain.cashflow.vo.CashflowForecastVo;
import com.jaedaero.domain.cashflow.vo.CashflowInputSourceVo;
import com.jaedaero.domain.cashflow.vo.SoldierSavingInputSourceVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DbCashflowInputProviderTest {

  @Test
  void nullBalancesAndSpendingAreTreatedAsZero() {
    CashflowInputSourceVo source = validSource();
    source.setBaseAsset(null);
    source.setMonthlySpendingAverage(null);
    CashflowInput result = new DbCashflowInputProvider(new StubMapper(source)).load(1L);

    assertEquals(0L, result.baseAsset());
    assertEquals(0L, result.monthlySpendingAverage());
    assertEquals(SoldierType.ARMY, result.soldierType());
  }

  @Test
  void missingGoalOrMilitaryProfileIsRejected() {
    assertThrows(
        CashflowException.class, () -> new DbCashflowInputProvider(new StubMapper(null)).load(1L));
  }

  @Test
  void invalidSoldierTypeIsRejected() {
    CashflowInputSourceVo source = validSource();
    source.setSoldierType("SPACE_FORCE");

    assertThrows(
        CashflowException.class,
        () -> new DbCashflowInputProvider(new StubMapper(source)).load(1L));
  }

  @Test
  void latestAiStrategyIsMappedAsAppliedCashflowInput() {
    CashflowInputSourceVo source = validSource();
    source.setActiveStrategyApplicationId(9L);
    source.setAppliedMonthlySpendingAmount(120_000L);
    source.setAppliedMonthlySavingAmount(550_000L);
    source.setAppliedMonthlyInvestmentAmount(80_000L);
    source.setAppliedExpectedReturnRate(new BigDecimal("5.00"));

    CashflowInput result = new DbCashflowInputProvider(new StubMapper(source)).load(1L);

    assertEquals(9L, result.appliedStrategy().applicationId());
    assertEquals(120_000L, result.appliedStrategy().monthlySpendingAmount());
    assertEquals(550_000L, result.appliedStrategy().monthlySavingAmount());
    assertEquals(80_000L, result.appliedStrategy().monthlyInvestmentAmount());
    assertEquals(new BigDecimal("5.00"), result.appliedStrategy().expectedReturnRate());
  }

  @Test
  void incompleteActiveAiStrategyIsRejected() {
    CashflowInputSourceVo source = validSource();
    source.setActiveStrategyApplicationId(9L);
    source.setAppliedMonthlySpendingAmount(120_000L);

    assertThrows(
        CashflowException.class,
        () -> new DbCashflowInputProvider(new StubMapper(source)).load(1L));
  }

  private CashflowInputSourceVo validSource() {
    CashflowInputSourceVo source = new CashflowInputSourceVo();
    source.setBaseAsset(1_000_000L);
    source.setTargetAmount(10_000_000L);
    source.setMonthlySpendingAverage(100_000L);
    source.setSoldierType("ARMY");
    source.setEnlistmentDate(LocalDate.of(2026, 1, 1));
    source.setDischargeDate(LocalDate.of(2027, 6, 1));
    return source;
  }

  private static class StubMapper implements CashflowMapper {
    private final CashflowInputSourceVo source;

    private StubMapper(CashflowInputSourceVo source) {
      this.source = source;
    }

    @Override
    public CashflowInputSourceVo findInputByUserId(long userId) {
      return source;
    }

    @Override
    public List<SoldierSavingInputSourceVo> findSoldierSavingsByUserId(long userId) {
      return List.of();
    }

    @Override
    public void insertForecast(CashflowForecastVo forecast) {
      throw new UnsupportedOperationException();
    }

    @Override
    public void insertForecastMonths(long forecastId, List<CashflowForecastMonthVo> months) {
      throw new UnsupportedOperationException();
    }

    @Override
    public CashflowForecastVo findLatestForecastByUserId(long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public List<CashflowForecastMonthVo> findMonthsByForecastId(long forecastId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public LocalDate findDischargeDateByUserId(long userId) {
      throw new UnsupportedOperationException();
    }
  }
}
