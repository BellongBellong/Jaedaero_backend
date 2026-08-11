package com.jaedaero.domain.aianalysis.llm;

/** AI 기능의 성격에 따라 서버가 사용할 모델을 고정한다. */
public enum AiGenerationTask {
  AI_COACH(OpenAiModel.GPT_4O_MINI),
  TRANSACTION_CATEGORY(OpenAiModel.GPT_5_NANO),
  DISCHARGE_REPORT(OpenAiModel.GPT_4O_MINI);

  private final OpenAiModel model;

  AiGenerationTask(OpenAiModel model) {
    this.model = model;
  }

  public OpenAiModel model() {
    return model;
  }
}
