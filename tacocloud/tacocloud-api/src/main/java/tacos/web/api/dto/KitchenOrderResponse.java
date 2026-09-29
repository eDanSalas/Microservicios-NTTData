package tacos.web.api.dto;

import java.util.Date;
import java.util.List;

import lombok.Value;
import tacos.OrderStatus;

@Value
public class KitchenOrderResponse {
  String id;
  Date placedAt;
  OrderStatus status;
  String stationId;
  String cookId;
  Date acceptedAt;
  int estimatedPrepMinutes;
  List<KitchenOrderLineResponse> items;
}
