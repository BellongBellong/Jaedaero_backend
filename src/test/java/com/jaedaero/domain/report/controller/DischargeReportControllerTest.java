package com.jaedaero.domain.report.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.report.dto.DischargeReportResponse;
import com.jaedaero.domain.report.service.DischargeReportService;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.AuthorityUtils;
import org.springframework.web.server.ResponseStatusException;

class DischargeReportControllerTest {

  @Test
  void returnsReportForAuthenticatedUser() {
    RecordingReportService service = new RecordingReportService();
    DischargeReportController controller = new DischargeReportController(service);

    var response = controller.getDischargeReport(authentication("7"));

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(7L, service.userId);
  }

  @Test
  void rejectsAbsentAnonymousAndInvalidAuthentication() {
    DischargeReportController controller = new DischargeReportController(new RecordingReportService());
    AnonymousAuthenticationToken anonymous = new AnonymousAuthenticationToken(
        "key", "anonymous", AuthorityUtils.createAuthorityList("ROLE_ANONYMOUS"));

    assertUnauthorized(() -> controller.getDischargeReport(null));
    assertUnauthorized(() -> controller.getDischargeReport(anonymous));
    assertUnauthorized(() -> controller.getDischargeReport(authentication("not-a-number")));
  }

  private void assertUnauthorized(Runnable request) {
    ResponseStatusException exception = assertThrows(ResponseStatusException.class, request::run);
    assertEquals(HttpStatus.UNAUTHORIZED, exception.getStatus());
  }

  private UsernamePasswordAuthenticationToken authentication(String name) {
    return new UsernamePasswordAuthenticationToken(name, null, List.of());
  }

  private static class RecordingReportService implements DischargeReportService {
    private long userId;

    @Override
    public DischargeReportResponse get(long userId) {
      this.userId = userId;
      return DischargeReportResponse.builder().build();
    }
  }
}
