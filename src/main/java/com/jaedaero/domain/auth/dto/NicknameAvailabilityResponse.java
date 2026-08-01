package com.jaedaero.domain.auth.dto;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public class NicknameAvailabilityResponse {

  private final boolean available;
}
