package com.jaedaero.domain.aianalysis.llm;

/** LLM이 생성한 자연어 영역. 금액·비율·날짜 등 계산값은 포함하지 않는다. */
public record AiCoachNarrative(String comment, String recommendReason) {}
