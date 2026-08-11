package com.jaedaero.domain.report.service;

import com.jaedaero.domain.report.dto.DischargeReportResponse;

public interface DischargeReportService {

  DischargeReportResponse get(long userId);
}
