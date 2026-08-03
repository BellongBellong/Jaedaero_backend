package com.jaedaero.domain.aianalysis.dto;

/** AI 코치 문구가 이번 응답에 제공된 경로. */
public enum AiGenerationSource {
  OPENAI,
  FALLBACK,
  CACHE
}
