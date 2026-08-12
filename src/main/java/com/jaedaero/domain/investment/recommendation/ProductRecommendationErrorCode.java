package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.global.common.exception.ErrorCode;
import lombok.Getter;
import org.springframework.http.HttpStatus;

@Getter
public enum ProductRecommendationErrorCode implements ErrorCode {
  INVALID_CONTEXT(HttpStatus.BAD_REQUEST, "PRODUCT_RECOMMENDATION_INVALID_CONTEXT"),
  CONTEXT_NOT_READY(HttpStatus.CONFLICT, "PRODUCT_RECOMMENDATION_CONTEXT_NOT_READY");

  private final HttpStatus status;
  private final String code;

  ProductRecommendationErrorCode(HttpStatus status, String code) {
    this.status = status;
    this.code = code;
  }
}
