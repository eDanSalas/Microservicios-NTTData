package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.Date;

import lombok.Value;
import tacos.OrderStatus;

@Value
public class AdminOrderSummaryResponse {
  String id;
  Date placedAt;
  OrderStatus status;
  BigDecimal total;
  String currency;
  int itemCount;
  String userId;
}
