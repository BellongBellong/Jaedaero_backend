package com.jaedaero.domain.investment.recommendation;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.jaedaero.domain.aianalysis.dto.AiAnalysisResponse;
import com.jaedaero.domain.aianalysis.dto.AiRecommendedScenarioResponse;
import com.jaedaero.domain.aianalysis.service.AiAnalysisService;
import com.jaedaero.domain.simulation.dto.SimulationResponse;
import com.jaedaero.domain.simulation.dto.SimulationDefaultsResponse;
import com.jaedaero.domain.simulation.service.SimulationService;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;

class ProductRecommendationContextResolverTest {

  @Test
  void resolvesAnalysisContextThroughUserScopedDetailLookup() {
    long[] requestedUserId = {0L};
    ProductRecommendationContextResolver resolver =
        new ProductRecommendationContextResolver(
            new AiAnalysisService() {
              @Override
              public AiAnalysisResponse analyze(long userId, com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest request) {
                throw new UnsupportedOperationException();
              }

              @Override
              public AiAnalysisResponse getDetail(long userId, long analysisId) {
                requestedUserId[0] = userId;
                return AiAnalysisResponse.builder()
                    .analysisId(analysisId)
                    .simulationId(4L)
                    .financialDischargeDate(LocalDate.of(2027, 3, 1))
                    .recommendedScenario(
                        AiRecommendedScenarioResponse.builder()
                            .monthlyInvestmentAmount(120_000L)
                            .expectedReturnRate(new BigDecimal("5.00"))
                            .build())
                    .build();
              }
            },
            unusedSimulationService());

    ProductRecommendationContext context = resolver.resolve(9L, 12L, null);

    assertEquals(9L, requestedUserId[0]);
    assertEquals(12L, context.analysisId());
    assertEquals(4L, context.simulationId());
    assertEquals(120_000L, context.monthlyInvestmentBudget());
    assertEquals(LocalDate.of(2027, 3, 1), context.financialDischargeDate());
  }

  @Test
  void rejectsAmbiguousContext() {
    ProductRecommendationContextResolver resolver =
        new ProductRecommendationContextResolver(unusedAiAnalysisService(), unusedSimulationService());

    ProductRecommendationException exception =
        assertThrows(ProductRecommendationException.class, () -> resolver.resolve(1L, 2L, 3L));

    assertEquals(ProductRecommendationErrorCode.INVALID_CONTEXT, exception.getErrorCode());
  }

  private static AiAnalysisService unusedAiAnalysisService() {
    return new AiAnalysisService() {
      @Override
      public AiAnalysisResponse analyze(long userId, com.jaedaero.domain.aianalysis.dto.AiAnalysisRequest request) {
        throw new UnsupportedOperationException();
      }

      @Override
      public AiAnalysisResponse getDetail(long userId, long analysisId) {
        throw new UnsupportedOperationException();
      }
    };
  }

  private static SimulationService unusedSimulationService() {
    return new SimulationService() {
      @Override
      public SimulationDefaultsResponse getDefaults(long userId) {
        throw new UnsupportedOperationException();
      }

      @Override
      public SimulationResponse run(long userId, com.jaedaero.domain.simulation.dto.SimulationRequest request) {
        throw new UnsupportedOperationException();
      }

      @Override
      public com.jaedaero.domain.simulation.dto.SimulationHistoryResponse getHistory(long userId, int page, int size) {
        throw new UnsupportedOperationException();
      }

      @Override
      public SimulationResponse getDetail(long userId, long simulationId) {
        throw new UnsupportedOperationException();
      }
    };
  }
}
