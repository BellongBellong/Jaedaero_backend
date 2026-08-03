package com.jaedaero.domain.aianalysis.llm;

/** 서버의 기능별 라우팅 정책에서 사용하는 OpenAI 모델만 표현한다. */
public enum OpenAiModel {
  GPT_4O_MINI("gpt-4o-mini"),
  GPT_5_NANO("gpt-5-nano");

  private final String apiName;

  OpenAiModel(String apiName) {
    this.apiName = apiName;
  }

  public String apiName() {
    return apiName;
  }
}
