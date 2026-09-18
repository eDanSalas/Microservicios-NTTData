package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tacos.OrderStatus;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderResponse {

  private String id;
  private Date placedAt;
  private OrderStatus status;
  private String userId;

  private String deliveryName;
  private String deliveryStreet;
  private String deliveryCity;
  private String deliveryState;
  private String deliveryZip;

  private PaymentMethodSummaryResponse paymentMethod;

  private BigDecimal subtotal;
  private BigDecimal discount;
  private BigDecimal total;
  private String currency;
  private String couponCode;
  private List<OrderItemResponse> items =
      new ArrayList<>();
}
