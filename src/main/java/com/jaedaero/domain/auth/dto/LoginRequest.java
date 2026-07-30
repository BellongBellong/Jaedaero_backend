package com.jaedaero.domain.auth.dto;

import javax.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class LoginRequest {

  @NotBlank
  private String socialType;

  @NotBlank private String authorizationCode;

  @NotBlank private String redirectUri;

}
