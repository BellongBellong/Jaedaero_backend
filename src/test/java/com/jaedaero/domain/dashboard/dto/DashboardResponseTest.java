package com.jaedaero.domain.dashboard.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.jaedaero.domain.cashflow.dto.CashflowForecastMonthResponse;
import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationSourceType;
import com.jaedaero.domain.strategyapplication.vo.StrategyApplicationVo;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;

class DashboardResponseTest {

  @Test
  void mapsLatestCashflowAndCurrentMonthValues() {
    LocalDate today = LocalDate.of(2026, 8, 4);
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder()
            .baseAsset(3_000_000L)
            .expectedAsset(15_000_000L)
            .achievementRate(new BigDecimal("75.00"))
            .financialDischargeDate(LocalDate.of(2027, 8, 20))
            .months(
                List.of(
                    CashflowForecastMonthResponse.builder()
                        .forecastMonth(LocalDate.of(2026, 8, 1))
                        .expectedSalary(1_905_000L)
                        .expectedSavingAmount(550_000L)
                        .expectedInvestmentAmount(420_000L)
                        .expectedSpendingAmount(154_000L)
                        .build()))
            .build();
    StrategyApplicationVo application =
        StrategyApplicationVo.builder()
            .sourceType(StrategyApplicationSourceType.SIMULATION)
            .appliedMonthlySavingAmount(1_000_000L)
            .appliedMonthlyInvestmentAmount(500_000L)
            .appliedMonthlySpendingAmount(100_000L)
            .build();

    DashboardResponse response =
        DashboardResponse.from(cashflow, LocalDate.of(2027, 9, 1), application, today);

    assertEquals(12L, response.getDeltaDaysVsActual());
    assertEquals(3_000_000L, response.getCurrentAsset());
    assertEquals(15_000_000L, response.getExpectedAsset());
    assertEquals(1_905_000L, response.getThisMonthIncome());
    assertEquals(420_000L, response.getThisMonthInvestment());
    assertEquals(154_000L, response.getThisMonthSpending());
    assertEquals(500_000L, response.getMonthlyInvestmentGoal());
    assertEquals(100_000L, response.getMonthlySpendingGoal());
    assertEquals(new BigDecimal("84.00"), response.getInvestmentGoalAchievementRate());
    assertEquals(new BigDecimal("154.00"), response.getSpendingGoalAchievementRate());
    assertEquals("SIMULATION", response.getGoalSource());
  }

  @Test
  void returnsNullGoalFieldsWhenNoStrategyHasBeenApplied() {
    CashflowForecastResponse cashflow =
        CashflowForecastResponse.builder().months(List.of()).build();

    DashboardResponse response =
        DashboardResponse.from(cashflow, LocalDate.of(2027, 9, 1), null, LocalDate.of(2026, 8, 4));

    assertEquals(null, response.getMonthlyInvestmentGoal());
    assertEquals(null, response.getMonthlySpendingGoal());
    assertEquals(null, response.getInvestmentGoalAchievementRate());
    assertEquals(null, response.getSpendingGoalAchievementRate());
    assertEquals(null, response.getGoalSource());
    assertEquals(null, response.getGoalAppliedAt());
  }
}
