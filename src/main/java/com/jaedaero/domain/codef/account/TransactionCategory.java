package com.jaedaero.domain.codef.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import java.util.Arrays;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

/** 사용자가 거래에 선택할 수 있는 고정 거래 분류입니다. */
@ApiModel(description = "거래 카테고리")
public enum TransactionCategory {
  SALARY("급여"),
  ASSET("자산"),
  PX("PX", Set.of("PX", "충성마트", "국군복지단", "영외마트", "나라사랑마트")),
  FOOD(
      "식비",
      Set.of(
          "스타벅스", "투썸", "이디야", "메가커피", "컴포즈", "빽다방", "맥도날드", "버거킹", "롯데리아",
          "맘스터치", "서브웨이", "배달의민족", "배민", "요기요", "쿠팡이츠", "식당", "카페", "치킨", "피자")),
  SHOPPING(
      "쇼핑",
      Set.of(
          "쿠팡", "마켓컬리", "컬리", "올리브영", "다이소", "무신사", "지마켓", "G마켓", "11번가",
          "오늘의집", "이케아", "홈플러스", "이마트", "롯데마트")),
  TRANSPORT(
      "교통",
      Set.of(
          "카카오T", "카카오택시", "택시", "티머니", "코레일", "KTX", "SRT", "고속버스", "시외버스",
          "쏘카", "그린카", "주차")),
  LEISURE(
      "여가",
      Set.of(
          "넷플릭스", "유튜브", "멜론", "스포티파이", "PC방", "CGV", "메가박스", "롯데시네마", "야놀자",
          "여기어때", "노래방", "에버랜드", "롯데월드")),
  MEDICAL("의료", Set.of("약국", "병원", "의원", "치과", "한의원", "보건소")),
  ETC("기타");

  private final String displayName;
  private final Set<String> merchantNames;

  TransactionCategory(String displayName) {
    this(displayName, Set.of());
  }

  TransactionCategory(String displayName, Set<String> merchantNames) {
    this.displayName = displayName;
    this.merchantNames =
        merchantNames.stream()
            .map(TransactionCategory::normalize)
            .collect(Collectors.toUnmodifiableSet());
  }

  public String getDisplayName() {
    return displayName;
  }

  /** 출금 설명을 규칙 기반 지출 분류로 변환합니다. */
  public static TransactionCategory fromDescription(String description) {
    String normalizedDescription = normalize(description);
    if (normalizedDescription.isBlank()) {
      return ETC;
    }
    return Arrays.stream(values())
        .filter(category -> !category.merchantNames.isEmpty())
        .filter(category -> category.merchantNames.stream().anyMatch(normalizedDescription::contains))
        .findFirst()
        .orElse(ETC);
  }

  /** API 코드(예: FOOD)와 한국어 표시명(식비)을 모두 허용합니다. */
  @JsonCreator
  public static TransactionCategory from(String value) {
    if (value == null) {
      return null;
    }
    return Arrays.stream(values())
        .filter(category -> category.name().equalsIgnoreCase(value) || category.displayName.equals(value))
        .findFirst()
        .orElseThrow(
            () ->
                new IllegalArgumentException(
                    "지원하지 않는 거래 카테고리입니다. 허용값: "
                        + Arrays.toString(TransactionCategory.values())));
  }

  private static String normalize(String value) {
    if (value == null) {
      return "";
    }
    return value.replaceAll("[\\s\\p{Punct}]", "").toUpperCase(Locale.ROOT);
  }
}
