package com.jaedaero.domain.investment.recommendation;

import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import io.swagger.annotations.ApiParam;
import io.swagger.annotations.ApiResponse;
import io.swagger.annotations.ApiResponses;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@Api(tags = "상품 추천")
@RequestMapping("/api/v1/products")
public class ProductRecommendationController {

  private final ProductRecommendationService productRecommendationService;

  public ProductRecommendationController(ProductRecommendationService productRecommendationService) {
    this.productRecommendationService = productRecommendationService;
  }

  @GetMapping("/recommendations")
  @ApiOperation(
      value = "ETF 위험등급별 상품 분류 조회",
      notes = "KRX ETF 시세를 LOW·MEDIUM·HIGH·VERY_HIGH로 분류합니다. VERY_HIGH(레버리지·인버스·파생형)는 초기 리밸런싱 추천에서 제외됩니다.",
      response = ProductRecommendationResponse.class)
  @ApiResponses({
    @ApiResponse(code = 200, message = "조회 성공", response = ProductRecommendationResponse.class),
    @ApiResponse(code = 400, message = "기준일 형식 또는 값이 올바르지 않음"),
    @ApiResponse(code = 404, message = "기준일 ETF 데이터를 찾을 수 없음"),
    @ApiResponse(code = 502, message = "KRX API 요청 또는 응답 처리 실패"),
    @ApiResponse(code = 503, message = "KRX 인증키 미설정 또는 KRX 요청 중단")
  })
  public ProductRecommendationResponse getRecommendations(
      Authentication authentication,
      @ApiParam(value = "기준일(yyyyMMdd). 생략하면 오늘", example = "20260806")
          @RequestParam(required = false)
          String asOfDate) {
    LocalDate date = asOfDate == null || asOfDate.isBlank() ? LocalDate.now() : parseDate(asOfDate);
    if (date.isAfter(LocalDate.now())) {
      throw new IllegalArgumentException("기준일은 오늘보다 늦을 수 없습니다.");
    }
    return productRecommendationService.getEtfRecommendations(authenticatedUserId(authentication), date);
  }

  private long authenticatedUserId(Authentication authentication) {
    if (authentication == null || !authentication.isAuthenticated()
        || authentication instanceof AnonymousAuthenticationToken) {
      throw new IllegalStateException("로그인이 필요합니다.");
    }
    return Long.parseLong(authentication.getName());
  }

  private LocalDate parseDate(String value) {
    if (!value.matches("\\d{8}")) {
      throw new IllegalArgumentException("기준일자는 yyyyMMdd 형식이어야 합니다.");
    }
    try {
      return LocalDate.parse(value, DateTimeFormatter.BASIC_ISO_DATE);
    } catch (RuntimeException exception) {
      throw new IllegalArgumentException("기준일자가 올바르지 않습니다.");
    }
  }
}
