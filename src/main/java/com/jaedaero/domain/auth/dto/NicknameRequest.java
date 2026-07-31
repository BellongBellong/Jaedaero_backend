package com.jaedaero.domain.auth.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class NicknameRequest {

  @NotBlank
  @Size(max = 50)
  private String nickname;
}
