package com.jaedaero.domain.investmentguidance.controller;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceApplyRequest;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceDetailResponse;
import com.jaedaero.domain.investmentguidance.dto.InvestmentGuidanceResponse;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceErrorCode;
import com.jaedaero.domain.investmentguidance.exception.InvestmentGuidanceException;
import com.jaedaero.domain.investmentguidance.service.InvestmentGuidanceService;
import com.jaedaero.domain.strategyapplication.dto.StrategyApplicationResponse;
import com.jaedaero.global.common.exception.GlobalExceptionHandler;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class InvestmentGuidanceControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    mockMvc =
        MockMvcBuilders.standaloneSetup(new InvestmentGuidanceController(new DetailService()))
            .setControllerAdvice(new GlobalExceptionHandler())
            .build();
  }

  @Test
  void unauthenticatedDetailRequestReturns401() throws Exception {
    mockMvc
        .perform(get("/api/v1/investment-guidances/10"))
        .andExpect(status().isUnauthorized())
        .andExpect(content().string(containsString("INVESTMENT_GUIDANCE_UNAUTHENTICATED")));
  }

  @Test
  void anotherUsersDetailRequestReturns404() throws Exception {
    mockMvc
        .perform(
            get("/api/v1/investment-guidances/10")
                .principal(
                    new UsernamePasswordAuthenticationToken(
                        "2", "password", Collections.emptyList())))
        .andExpect(status().isNotFound())
        .andExpect(content().string(containsString("INVESTMENT_GUIDANCE_NOT_FOUND")));
  }

  private static class DetailService implements InvestmentGuidanceService {

    @Override
    public InvestmentGuidanceResponse getLatest(long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public InvestmentGuidanceDetailResponse getDetail(long userId, long guidanceId) {
      if (userId != 1L || guidanceId != 10L) {
        throw new InvestmentGuidanceException(
            InvestmentGuidanceErrorCode.NOT_FOUND, "조회할 적립식 투자 가이드를 찾을 수 없습니다.");
      }
      return InvestmentGuidanceDetailResponse.builder().guidanceId(guidanceId).build();
    }

    @Override
    public InvestmentGuidanceResponse create(long userId) {
      throw new UnsupportedOperationException();
    }

    @Override
    public StrategyApplicationResponse apply(
        long userId, long guidanceId, InvestmentGuidanceApplyRequest request) {
      throw new UnsupportedOperationException();
    }
  }
}
