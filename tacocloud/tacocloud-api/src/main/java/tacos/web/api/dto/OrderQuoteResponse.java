package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Value;

@Value
public class OrderQuoteResponse {
  String couponCode;
  BigDecimal subtotal;
  BigDecimal discount;
  BigDecimal total;
  String currency;
  List<TacoClassificationResponse> classifications;
}
