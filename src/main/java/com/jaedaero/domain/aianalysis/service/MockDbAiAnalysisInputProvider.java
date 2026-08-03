package com.jaedaero.domain.aianalysis.service;

import com.jaedaero.domain.aianalysis.exception.AiAnalysisErrorCode;
import com.jaedaero.domain.aianalysis.exception.AiAnalysisException;
import com.jaedaero.domain.aianalysis.mapper.AiAnalysisInputMapper;
import com.jaedaero.domain.aianalysis.vo.AiAnalysisInputSourceVo;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

@Primary
@Component
@RequiredArgsConstructor
public class MockDbAiAnalysisInputProvider implements AiAnalysisInputProvider {
  private final AiAnalysisInputMapper mapper;
  @Override public AiAnalysisInput load(long userId) {
    AiAnalysisInputSourceVo s = mapper.findLatestByUserId(userId);
    if (s == null || s.getForecastId() == null || s.getExpectedAsset() == null || s.getTargetAmount() == null || s.getActualDischargeDate() == null) {
      throw new AiAnalysisException(AiAnalysisErrorCode.INPUT_NOT_READY, "AI 분석에 필요한 캐시플로우, 목표 또는 복무 정보가 없습니다.");
    }
    return new AiAnalysisInput(s.getForecastId(), s.getSnapshotId(), s.getExpectedAsset(), s.getFinancialDischargeDate(),
        s.getAchievementRate() == null ? BigDecimal.ZERO : s.getAchievementRate(), s.getTargetAmount(), s.getActualDischargeDate(),
        s.getMonthlySpendingLimit() == null ? 0L : s.getMonthlySpendingLimit());
  }
}
