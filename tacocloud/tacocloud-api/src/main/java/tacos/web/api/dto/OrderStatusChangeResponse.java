package tacos.web.api.dto;

import java.util.Date;

import lombok.Value;
import tacos.OrderStatus;

@Value
public class OrderStatusChangeResponse {
  OrderStatus fromStatus;
  OrderStatus toStatus;
  Date changedAt;
  String actorId;
  String actorRole;
  String origin;
  String reason;
}
