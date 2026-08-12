package com.jaedaero.domain.codef.account;

/** 계좌번호를 API·저장용 표시 형식으로 마스킹합니다. */
public final class AccountNumberMasker {

  private AccountNumberMasker() {}

  public static String mask(String value) {
    if (value == null || value.isBlank()) return null;
    String normalized = value.replaceAll("[^0-9A-Za-z]", "");
    if (value.contains("*")) {
      return "****-**-****" + normalized.substring(Math.max(0, normalized.length() - 2));
    }
    if (normalized.length() <= 8) return "****-**-****";
    return normalized.substring(0, 6) + "-**-****" + normalized.substring(normalized.length() - 2);
  }
}
