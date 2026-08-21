package com.jaedaero.domain.notification.dto;

import java.util.List;
import lombok.Builder;
import lombok.Getter;

@Getter
@Builder
public class NotificationPageResponse {
  private final List<NotificationItemResponse> items;
  private final int page;
  private final int size;
  private final long totalElements;
  private final boolean hasNext;
}
