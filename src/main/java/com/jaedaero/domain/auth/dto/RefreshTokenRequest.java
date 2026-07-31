package com.jaedaero.domain.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class RefreshTokenRequest {

  @NotBlank private String refreshToken;
}
