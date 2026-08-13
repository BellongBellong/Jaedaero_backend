package com.jaedaero.domain.investment.recommendation;

import com.jaedaero.global.common.exception.BusinessException;

public class ProductRecommendationException extends BusinessException {

  public ProductRecommendationException(ProductRecommendationErrorCode errorCode, String message) {
    super(errorCode, message);
  }
}
