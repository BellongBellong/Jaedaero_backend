package com.jaedaero.domain.auth.event;

/** 온보딩 마지막 단계의 투자성향·목표 금액 저장이 커밋된 뒤 발행됩니다. */
public record OnboardingCompletedEvent(long userId) {}
