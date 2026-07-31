package com.jaedaero.domain.auth.dto;

import com.jaedaero.domain.auth.common.enums.SoldierType;
import com.fasterxml.jackson.annotation.JsonFormat;
import java.time.LocalDate;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.PastOrPresent;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MilitaryInfoRequest {

  @NotNull private SoldierType soldierType;

  @NotBlank
  @Size(max = 20)
  private String rankName;

  @NotNull
  @PastOrPresent
  @JsonFormat(pattern = "yyyy.MM.dd")
  private LocalDate enlistmentDate;
}
