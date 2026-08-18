package com.jaedaero.domain.leavebenefit;

import io.swagger.annotations.ApiModel;
import io.swagger.annotations.ApiModelProperty;
import java.time.LocalDate;

@ApiModel(description = "휴가 혜택")
public record LeaveBenefitResponse(
    @ApiModelProperty(value = "혜택 ID", example = "1") long benefitId,
    @ApiModelProperty(value = "휴가 혜택 카테고리", example = "LEISURE") LeaveBenefitCategory category,
    @ApiModelProperty(value = "혜택명", example = "한국민속촌 할인") String title,
    @ApiModelProperty(value = "카드 목록용 할인 요약", example = "대중교통 20%, 카카오T 택시 10% 캐시백")
        String discountSummary,
    @ApiModelProperty(value = "혜택 시작일", example = "2026-09-01") LocalDate validFrom,
    @ApiModelProperty(value = "혜택 종료일", example = "2026-10-31") LocalDate validTo,
    @ApiModelProperty(value = "대상", example = "현역 간부 및 병사") String target,
    @ApiModelProperty(value = "혜택 내용", example = "한국민속촌 할인가 적용") String content,
    @ApiModelProperty(value = "이용 방법", example = "현장 매표소에서 군인 신분증을 제시합니다.") String method,
    @ApiModelProperty(value = "유의사항(전월 실적·월 한도·횟수 제한·급여이체 조건 등)")
        String precautions,
    @ApiModelProperty(value = "상세 안내 이미지 URL") String detailImageUrl,
    @ApiModelProperty(value = "공식 출처 URL") String sourceUrl) {}
