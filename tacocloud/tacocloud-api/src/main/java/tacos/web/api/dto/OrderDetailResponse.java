package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import lombok.Value;
import tacos.OrderStatus;

@Value
public class OrderDetailResponse {
  String id;
  Date placedAt;
  OrderStatus status;
  String deliveryName;
  String deliveryStreet;
  String deliveryCity;
  String deliveryState;
  String deliveryZip;
  PaymentMethodSummaryResponse paymentMethod;
  BigDecimal subtotal;
  BigDecimal discount;
  BigDecimal total;
  String currency;
  List<OrderItemResponse> items;
  List<OrderStatusChangeResponse> statusHistory;
}
