package com.jaedaero.domain.aianalysis.llm;

public interface AiCoachNarrativeGenerator {

  AiCoachNarrative generate(OpenAiModel model, String prompt);
}
