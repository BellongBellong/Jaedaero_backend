package com.jaedaero.domain.marketreport.service;

import com.jaedaero.domain.marketreport.dto.MarketReportGenerationSource;
import com.jaedaero.domain.marketreport.mapper.DailyMarketReportMapper;
import com.jaedaero.domain.marketreport.vo.DailyMarketReportVo;
import java.time.LocalDate;
import java.time.LocalDateTime;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** 날짜별 리포트 생성권을 별도 트랜잭션으로 선점해 외부 호출 중복을 막는다. */
@Component
public class MarketReportClaimService {

  static final String PENDING_PROMPT_VERSION = "pending";
  static final String IN_PROGRESS_PROMPT_VERSION = "in-progress";

  private final DailyMarketReportMapper reportMapper;

  public MarketReportClaimService(DailyMarketReportMapper reportMapper) {
    this.reportMapper = reportMapper;
  }

  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int claim(
      LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) {
    return claim(reportDate, validFrom, validUntil, false);
  }

  /** 로컬 명시적 재시험만 FALLBACK/pending 행을 다시 선점한다. */
  @Transactional(propagation = Propagation.REQUIRES_NEW)
  public int claimForLocalRetry(
      LocalDate reportDate, LocalDateTime validFrom, LocalDateTime validUntil) {
    return claim(reportDate, validFrom, validUntil, true);
  }

  private int claim(
      LocalDate reportDate,
      LocalDateTime validFrom,
      LocalDateTime validUntil,
      boolean allowFallbackRetry) {
    int inserted = reportMapper.claimReportDate(reportDate, validFrom, validUntil);
    if (inserted == 1) {
      return markInProgress(reportMapper.findByReportDateForUpdate(reportDate));
    }
    if (!allowFallbackRetry) {
      return 0;
    }

    DailyMarketReportVo existing = reportMapper.findByReportDateForUpdate(reportDate);
    if (existing == null
        || existing.getGenerationSource() == MarketReportGenerationSource.GEMINI
        || IN_PROGRESS_PROMPT_VERSION.equals(existing.getPromptVersion())
        || !isRetryable(existing)) {
      return 0;
    }
    return markInProgress(existing);
  }

  private int markInProgress(DailyMarketReportVo report) {
    if (report == null || report.getReportId() == null) {
      return 0;
    }
    return reportMapper.markGenerationInProgress(report.getReportId());
  }

  private boolean isRetryable(DailyMarketReportVo report) {
    return report.getGenerationSource() == MarketReportGenerationSource.FALLBACK
        || PENDING_PROMPT_VERSION.equals(report.getPromptVersion());
  }
}
