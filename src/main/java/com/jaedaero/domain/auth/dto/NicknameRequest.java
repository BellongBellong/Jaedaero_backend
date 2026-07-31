package com.jaedaero.domain.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameRequest {

  @NotBlank
  @Size(max = 12)
  @Pattern(regexp = "[가-힣a-zA-Z]{2,12}")
  private String nickname;
}
