package com.jaedaero.domain.aianalysis.llm;

/** LLM 네트워크 호출 또는 응답 해석 실패. AI 분석은 템플릿으로 정상 대체한다. */
public class AiCoachNarrativeGenerationException extends RuntimeException {

  public AiCoachNarrativeGenerationException(String message) {
    super(message);
  }

  public AiCoachNarrativeGenerationException(String message, Throwable cause) {
    super(message, cause);
  }
}
