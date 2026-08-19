package com.jaedaero.domain.leavemode.dto;

import com.fasterxml.jackson.annotation.JsonAlias;
import java.time.LocalDate;
import javax.validation.constraints.AssertTrue;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PositiveOrZero;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class LeaveModeRequest {

  @NotBlank(message = "이벤트명은 필수입니다.")
  @Size(max = 100, message = "이벤트명은 100자 이하여야 합니다.")
  private String eventName;

  @NotNull(message = "휴가 시작일은 필수입니다.")
  private LocalDate startDate;

  @NotNull(message = "휴가 종료일은 필수입니다.")
  private LocalDate endDate;

  @JsonAlias("isLeaveModeEnabled")
  private boolean leaveModeEnabled = true;

  @PositiveOrZero(message = "휴가 예산은 0 이상이어야 합니다.")
  private Long budgetAmount;

  /** 휴가 시작일과 종료일의 선후 관계를 검증합니다. */
  @AssertTrue(message = "휴가 종료일은 시작일보다 빠를 수 없습니다.")
  public boolean isValidPeriod() {
    return startDate == null || endDate == null || !endDate.isBefore(startDate);
  }
}
