package com.jaedaero.domain.investmentguidance.vo;

import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class MockDbBrokeragePositionSourceVo {

  private Long accountValue;
  private Long cashBalance;
  private LocalDateTime marketDataAsOf;
}
