package com.jaedaero.domain.investmentguidance.vo;

public enum ServiceStage {
  PRIVATE_BASE,
  PRIVATE_FIRST_CLASS_GROWTH,
  CORPORAL_CHECK,
  SERGEANT_PREPARE;

  public static ServiceStage fromRankName(String rankName) {
    if (rankName == null) return PRIVATE_BASE;
    String normalized = rankName.trim();
    if (normalized.contains("병장")) return SERGEANT_PREPARE;
    if (normalized.contains("상병")) return CORPORAL_CHECK;
    if (normalized.contains("일병")) return PRIVATE_FIRST_CLASS_GROWTH;
    return PRIVATE_BASE;
  }
}
