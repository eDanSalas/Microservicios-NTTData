package tacos.pricing;

import java.math.BigDecimal;
import java.time.LocalDate;

import lombok.Data;

@Data
public class CouponRule {

  private CouponType type;
  private BigDecimal value;
  private LocalDate startsOn;
  private LocalDate expiresOn;
  private BigDecimal minimumSubtotal = BigDecimal.ZERO;
  private BigDecimal maxDiscount;
}
