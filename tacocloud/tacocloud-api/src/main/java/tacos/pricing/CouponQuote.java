package tacos.pricing;

import java.math.BigDecimal;

import lombok.Value;

@Value
public class CouponQuote {
  String code;
  BigDecimal subtotal;
  BigDecimal discount;
  BigDecimal total;
}
