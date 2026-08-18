package com.jaedaero.domain.leavebenefit;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import org.junit.jupiter.api.Test;

class LeaveBenefitServiceImplTest {

  @Test
  void usesSeoulDateAndForwardsOptionalCategory() {
    RecordingLeaveBenefitMapper mapper = new RecordingLeaveBenefitMapper();
    Clock clock = Clock.fixed(Instant.parse("2026-08-18T00:00:00Z"), ZoneId.of("Asia/Seoul"));
    LeaveBenefitService service = new LeaveBenefitServiceImpl(mapper, clock);

    service.getAvailableBenefits(LeaveBenefitCategory.LEISURE);

    assertEquals(LeaveBenefitCategory.LEISURE, mapper.category);
    assertEquals(LocalDate.of(2026, 8, 18), mapper.referenceDate);
  }

  private static class RecordingLeaveBenefitMapper implements LeaveBenefitMapper {
    private LeaveBenefitCategory category;
    private LocalDate referenceDate;

    @Override
    public List<LeaveBenefitResponse> findAvailableBenefits(
        LeaveBenefitCategory category, LocalDate referenceDate) {
      this.category = category;
      this.referenceDate = referenceDate;
      return List.of();
    }
  }
}
