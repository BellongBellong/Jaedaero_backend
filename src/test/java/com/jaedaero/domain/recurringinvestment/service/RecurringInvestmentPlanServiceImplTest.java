package com.jaedaero.domain.recurringinvestment.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanRequest;
import com.jaedaero.domain.recurringinvestment.dto.RecurringInvestmentPlanResponse;
import com.jaedaero.domain.recurringinvestment.exception.RecurringInvestmentPlanException;
import com.jaedaero.domain.recurringinvestment.mapper.RecurringInvestmentPlanMapper;
import com.jaedaero.domain.recurringinvestment.service.impl.RecurringInvestmentPlanServiceImpl;
import com.jaedaero.domain.recurringinvestment.vo.InvestmentFrequency;
import com.jaedaero.domain.recurringinvestment.vo.RecurringInvestmentPlanVo;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import org.junit.jupiter.api.Test;

class RecurringInvestmentPlanServiceImplTest {

  private static final Clock FIXED_CLOCK =
      Clock.fixed(Instant.parse("2026-08-04T00:00:00Z"), ZoneId.of("Asia/Seoul"));

  @Test
  void savesOwnedSecuritiesAccountAndCalculatesNextContributionDate() {
    InMemoryPlanMapper mapper = new InMemoryPlanMapper();
    RecurringInvestmentPlanService service =
        new RecurringInvestmentPlanServiceImpl(mapper, FIXED_CLOCK);

    RecurringInvestmentPlanResponse response =
        service.save(
            1L,
            new RecurringInvestmentPlanRequest(
                InvestmentFrequency.MONTHLY,
                10,
                150_000L,
                200_000L,
                7L,
                "069500",
                "KODEX 200"));

    assertEquals(1L, response.getPlanId());
    assertEquals(150_000L, response.getMonthlyEquivalentAmount());
    assertEquals(LocalDate.of(2026, 8, 10), response.getNextContributionDate());
    assertEquals("069500", response.getInvestmentProductCode());
  }

  @Test
  void rejectsWeeklyContributionWhoseMonthlyEquivalentExceedsLimit() {
    RecurringInvestmentPlanService service =
        new RecurringInvestmentPlanServiceImpl(new InMemoryPlanMapper(), FIXED_CLOCK);

    assertThrows(
        RecurringInvestmentPlanException.class,
        () ->
            service.save(
                1L,
                new RecurringInvestmentPlanRequest(
                    InvestmentFrequency.WEEKLY,
                    5,
                    50_000L,
                    200_000L,
                    7L,
                    "069500",
                    "KODEX 200")));
  }

  private static class InMemoryPlanMapper implements RecurringInvestmentPlanMapper {

    private RecurringInvestmentPlanVo plan;

    @Override
    public int insert(RecurringInvestmentPlanVo plan) {
      plan.setPlanId(1L);
      plan.setCreatedAt(LocalDateTime.of(2026, 8, 4, 9, 0));
      plan.setUpdatedAt(plan.getCreatedAt());
      this.plan = plan;
      return 1;
    }

    @Override
    public int update(RecurringInvestmentPlanVo plan) {
      this.plan = plan;
      return 1;
    }

    @Override
    public RecurringInvestmentPlanVo findByUserId(long userId) {
      return plan != null && plan.getUserId() == userId ? plan : null;
    }

    @Override
    public RecurringInvestmentPlanVo findByIdAndUserId(long planId, long userId) {
      return plan != null && plan.getPlanId() == planId && plan.getUserId() == userId ? plan : null;
    }

    @Override
    public boolean existsSecuritiesAccount(long accountId, long userId) {
      return accountId == 7L && userId == 1L;
    }
  }
}
