package com.jaedaero.domain.notification.push;

public record PushDeliveryResult(Status status, String detail) {
  public enum Status {
    SENT,
    SKIPPED,
    RETRYABLE_FAILURE,
    PERMANENT_FAILURE
  }

  public static PushDeliveryResult sent() {
    return new PushDeliveryResult(Status.SENT, null);
  }

  public static PushDeliveryResult skipped(String detail) {
    return new PushDeliveryResult(Status.SKIPPED, detail);
  }

  public static PushDeliveryResult retryable(String detail) {
    return new PushDeliveryResult(Status.RETRYABLE_FAILURE, detail);
  }

  public static PushDeliveryResult permanent(String detail) {
    return new PushDeliveryResult(Status.PERMANENT_FAILURE, detail);
  }
}
