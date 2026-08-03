package com.jaedaero.domain.cashflow.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.cashflow.dto.CashflowForecastResponse;
import com.jaedaero.domain.cashflow.exception.CashflowException;
import com.jaedaero.domain.cashflow.service.CashflowService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;

class CashflowControllerTest {

  @Test
  void generateUsesAuthenticatedUserAndReturnsCreated() {
    RecordingCashflowService service = new RecordingCashflowService();
    CashflowController controller = new CashflowController(service);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken("7", "password", Collections.emptyList());

    var response = controller.generate(authentication);

    assertEquals(HttpStatus.CREATED, response.getStatusCode());
    assertEquals(7L, service.generatedUserId);
  }

  @Test
  void anonymousUserIsRejected() {
    CashflowController controller = new CashflowController(new RecordingCashflowService());
    AnonymousAuthenticationToken anonymous =
        new AnonymousAuthenticationToken(
            "key", "anonymousUser", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    assertThrows(CashflowException.class, () -> controller.getLatest(anonymous));
  }

  private static class RecordingCashflowService implements CashflowService {
    private long generatedUserId;

    @Override
    public CashflowForecastResponse generate(long userId) {
      generatedUserId = userId;
      return CashflowForecastResponse.builder().forecastId(1L).build();
    }

    @Override
    public CashflowForecastResponse getLatest(long userId) {
      return CashflowForecastResponse.builder().forecastId(1L).build();
    }
  }
}
