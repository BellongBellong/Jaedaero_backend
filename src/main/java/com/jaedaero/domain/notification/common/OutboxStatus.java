package com.jaedaero.domain.notification.common;

public enum OutboxStatus {
  PENDING,
  PROCESSING,
  PUBLISHED,
  FAILED
}
