package com.jaedaero.domain.codef.account;

import com.fasterxml.jackson.annotation.JsonCreator;
import io.swagger.annotations.ApiModel;
import java.util.Arrays;

/** Fixed spending categories selectable by a user for a transaction. */
@ApiModel(description = "거래 카테고리")
public enum TransactionCategory {
  SALARY("급여"),
  ASSET("자산"),
  PX("PX"),
  FOOD("식비"),
  SHOPPING("쇼핑"),
  TRANSPORT("교통"),
  LEISURE("여가"),
  MEDICAL("의료"),
  ETC("기타");

  private final String displayName;

  TransactionCategory(String displayName) {
    this.displayName = displayName;
  }

  public String getDisplayName() {
    return displayName;
  }

  /** Accepts both API codes (for example, FOOD) and Korean display names (식비). */
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
}
