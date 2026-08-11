package com.jaedaero.domain.auth.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.jaedaero.domain.auth.common.enums.InvestmentPreference;
import com.jaedaero.domain.auth.dto.InvestmentPreferenceRequest;
import com.jaedaero.domain.auth.dto.InvestmentPreferenceResponse;
import com.jaedaero.domain.auth.exception.AuthErrorCode;
import com.jaedaero.domain.auth.exception.InvestmentPreferenceException;
import com.jaedaero.domain.auth.mapper.InvestmentPreferenceMapper;
import com.jaedaero.domain.auth.service.InvestmentPreferenceService;
import org.junit.jupiter.api.Test;

class InvestmentPreferenceServiceImplTest {

  @Test
  void storesPreferenceSeedAndTargetAmountTogether() {
    RecordingMapper mapper = new RecordingMapper();
    InvestmentPreferenceService service = new InvestmentPreferenceServiceImpl(mapper);
    InvestmentPreferenceRequest request = request(InvestmentPreference.BALANCED, 23_000_000L);

    InvestmentPreferenceResponse response =
        service.registerInvestmentPreference(1L, request);

    assertTrue(response.isSuccess());
    assertEquals(InvestmentPreference.BALANCED, response.getPreference());
    assertEquals(23_000_000L, response.getTargetAmount());
    assertEquals(1L, mapper.userId);
    assertEquals(InvestmentPreference.BALANCED, mapper.preference);
    assertEquals(23_000_000L, mapper.targetAmount);
  }

  @Test
  void rejectsZeroTargetAmountBeforeWriting() {
    RecordingMapper mapper = new RecordingMapper();
    InvestmentPreferenceService service = new InvestmentPreferenceServiceImpl(mapper);

    InvestmentPreferenceException exception =
        assertThrows(
            InvestmentPreferenceException.class,
            () ->
                service.registerInvestmentPreference(
                    1L, request(InvestmentPreference.SAFE, 0L)));

    assertEquals(AuthErrorCode.INVALID_TARGET_AMOUNT, exception.getErrorCode());
    assertEquals(0L, mapper.userId);
  }

  private InvestmentPreferenceRequest request(
      InvestmentPreference preference, long targetAmount) {
    InvestmentPreferenceRequest request = new InvestmentPreferenceRequest();
    request.setInvestmentPreference(preference);
    request.setTargetAmount(targetAmount);
    return request;
  }

  private static class RecordingMapper implements InvestmentPreferenceMapper {

    private long userId;
    private InvestmentPreference preference;
    private long targetAmount;

    @Override
    public int countActiveUserByUserId(long userId) {
      return 1;
    }

    @Override
    public InvestmentPreference findInitialPreferenceByUserId(long userId) {
      return preference;
    }

    @Override
    public void upsertInitialPreference(
        long userId, InvestmentPreference investmentPreference) {
      this.userId = userId;
      this.preference = investmentPreference;
    }

    @Override
    public void upsertGoalTargetAmount(long userId, long targetAmount) {
      this.userId = userId;
      this.targetAmount = targetAmount;
    }
  }
}
