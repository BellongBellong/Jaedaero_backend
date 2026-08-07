package com.jaedaero.domain.simulation.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.simulation.dto.SimulationDefaultsResponse;
import com.jaedaero.domain.simulation.dto.SimulationHistoryResponse;
import com.jaedaero.domain.simulation.dto.SimulationRequest;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.exception.SimulationException;
import com.jaedaero.domain.simulation.service.SimulationService;
import java.util.Collections;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;

class SimulationControllerTest {

  @Test
  void defaultsUsesAuthenticatedUserAndReturnsOk() {
    RecordingSimulationService service = new RecordingSimulationService();
    SimulationController controller = new SimulationController(service);
    UsernamePasswordAuthenticationToken authentication =
        new UsernamePasswordAuthenticationToken("7", "password", Collections.emptyList());

    var response = controller.getDefaults(authentication);

    assertEquals(HttpStatus.OK, response.getStatusCode());
    assertEquals(7L, service.defaultsUserId);
    assertEquals(550_000L, response.getBody().getMaxMonthlySavingAmount());
  }

  @Test
  void defaultsRejectsMissingAuthentication() {
    SimulationController controller =
        new SimulationController(new RecordingSimulationService());

    assertThrows(SimulationException.class, () -> controller.getDefaults(null));
  }

  private static class RecordingSimulationService implements SimulationService {

    private long defaultsUserId;

    @Override
    public SimulationDefaultsResponse getDefaults(long userId) {
      defaultsUserId = userId;
      return SimulationDefaultsResponse.builder().maxMonthlySavingAmount(550_000L).build();
    }

    @Override
    public SimulationResponse run(long userId, SimulationRequest request) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimulationHistoryResponse getHistory(long userId, int page, int size) {
      throw new UnsupportedOperationException();
    }

    @Override
    public SimulationResponse getDetail(long userId, long simulationId) {
      throw new UnsupportedOperationException();
    }
  }
}
