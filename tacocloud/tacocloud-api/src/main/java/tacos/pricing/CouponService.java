package tacos.pricing;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Clock;
import java.time.LocalDate;
import java.util.Locale;
import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import tacos.TacoOrder;

@Service
public class CouponService {

  private static final BigDecimal ONE_HUNDRED = new BigDecimal("100");
  private final CouponProperties properties;
  private final Clock clock;

  public CouponService(CouponProperties properties, Clock clock) {
    this.properties = properties;
    this.clock = clock;
  }

  public CouponQuote quote(BigDecimal subtotal, String rawCode) {
    BigDecimal amount = money(subtotal);
    String code = normalize(rawCode);
    CouponRule rule = findRule(code);
    validate(rule, amount);
    BigDecimal discount = calculateDiscount(rule, amount).min(amount);
    return new CouponQuote(code, amount, discount, amount.subtract(discount).setScale(2));
  }

  public void apply(TacoOrder order, String rawCode) {
    if (rawCode == null || rawCode.isBlank()) return;
    if (order.getCouponCode() != null) throw invalidCoupon();
    CouponQuote quote = quote(order.getSubtotal(), rawCode);
    order.setCouponCode(quote.getCode());
    order.setDiscount(quote.getDiscount());
    order.setTotal(quote.getTotal());
  }

  private CouponRule findRule(String code) {
    return properties.getCodes().entrySet().stream()
        .filter(entry -> normalize(entry.getKey()).equals(code))
        .map(Map.Entry::getValue).findFirst().orElseThrow(this::invalidCoupon);
  }

  private void validate(CouponRule rule, BigDecimal subtotal) {
    LocalDate today = LocalDate.now(clock);
    if (rule == null || rule.getType() == null || rule.getValue() == null
        || rule.getValue().signum() < 0 || rule.getStartsOn() != null && today.isBefore(rule.getStartsOn())
        || rule.getExpiresOn() != null && today.isAfter(rule.getExpiresOn())
        || subtotal.compareTo(valueOrZero(rule.getMinimumSubtotal())) < 0) throw invalidCoupon();
  }

  private BigDecimal calculateDiscount(CouponRule rule, BigDecimal subtotal) {
    BigDecimal discount = rule.getType() == CouponType.PERCENTAGE
        ? subtotal.multiply(rule.getValue()).divide(ONE_HUNDRED, 2, RoundingMode.HALF_UP)
        : money(rule.getValue());
    if (rule.getMaxDiscount() != null) discount = discount.min(money(rule.getMaxDiscount()));
    return discount.max(BigDecimal.ZERO).setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal money(BigDecimal value) {
    if (value == null || value.signum() < 0) throw invalidCoupon();
    return value.setScale(2, RoundingMode.HALF_UP);
  }

  private BigDecimal valueOrZero(BigDecimal value) {
    return value == null ? BigDecimal.ZERO : value;
  }

  private String normalize(String code) {
    if (code == null || code.isBlank()) throw invalidCoupon();
    return code.trim().toUpperCase(Locale.ROOT);
  }

  private ResponseStatusException invalidCoupon() {
    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, "Coupon cannot be applied");
  }
}
