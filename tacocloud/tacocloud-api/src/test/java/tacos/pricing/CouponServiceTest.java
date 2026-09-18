package tacos.pricing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.web.server.ResponseStatusException;

import tacos.TacoOrder;

public class CouponServiceTest {

  @ParameterizedTest
  @CsvSource({"PERCENTAGE,10,20.00", "FIXED,25,25.00"})
  public void shouldCalculateCouponByType(CouponType type, String value, String expected) {
    CouponQuote quote = service(rule(type, value, "0", null), "2026-06-15").quote(
        new BigDecimal("200.00"), " save10 ");
    assertEquals(new BigDecimal(expected), quote.getDiscount());
    assertEquals(new BigDecimal("200.00").subtract(new BigDecimal(expected)), quote.getTotal());
    assertEquals("SAVE10", quote.getCode());
  }

  @ParameterizedTest
  @CsvSource({"2026-01-01,10.00", "2026-12-31,10.00"})
  public void shouldAcceptCouponOnDateBoundaries(String date, String expected) {
    CouponQuote quote = service(rule(CouponType.PERCENTAGE, "10", "0", null), date)
        .quote(new BigDecimal("100.00"), "SaVe10");
    assertEquals(new BigDecimal(expected), quote.getDiscount());
  }

  @ParameterizedTest
  @CsvSource({"2025-12-31,SAVE10,100.00", "2027-01-01,SAVE10,100.00",
      "2026-06-15,UNKNOWN,100.00", "2026-06-15,SAVE10,49.99"})
  public void shouldUseSameResponseForInvalidCoupons(String date, String code, String subtotal) {
    ResponseStatusException error = assertThrows(ResponseStatusException.class, () -> service(
        rule(CouponType.PERCENTAGE, "10", "50", null), date).quote(new BigDecimal(subtotal), code));
    assertEquals("Coupon cannot be applied", error.getReason());
  }

  @Test
  public void shouldLimitDiscountAndNeverMakeTotalNegative() {
    CouponQuote capped = service(rule(CouponType.PERCENTAGE, "90", "0", "30"), "2026-06-15")
        .quote(new BigDecimal("100.00"), "SAVE10");
    CouponQuote zero = service(rule(CouponType.FIXED, "500", "0", null), "2026-06-15")
        .quote(new BigDecimal("100.00"), "SAVE10");
    assertEquals(new BigDecimal("30.00"), capped.getDiscount());
    assertEquals(new BigDecimal("0.00"), zero.getTotal());
  }

  @Test
  public void shouldRejectApplyingCouponTwice() {
    CouponService service = service(rule(CouponType.FIXED, "10", "0", null), "2026-06-15");
    TacoOrder order = new TacoOrder();
    order.setSubtotal(new BigDecimal("50.00"));
    service.apply(order, "SAVE10");
    assertEquals("SAVE10", order.getCouponCode());
    assertEquals(new BigDecimal("10.00"), order.getDiscount());
    assertEquals(new BigDecimal("40.00"), order.getTotal());
    assertThrows(ResponseStatusException.class, () -> service.apply(order, "SAVE10"));
  }

  private CouponService service(CouponRule rule, String date) {
    CouponProperties properties = new CouponProperties();
    properties.setCodes(Map.of("SAVE10", rule));
    Clock clock = Clock.fixed(Instant.parse(date + "T12:00:00Z"), ZoneOffset.UTC);
    return new CouponService(properties, clock);
  }

  private CouponRule rule(CouponType type, String value, String minimum, String maximum) {
    CouponRule rule = new CouponRule();
    rule.setType(type);
    rule.setValue(new BigDecimal(value));
    rule.setStartsOn(LocalDate.parse("2026-01-01"));
    rule.setExpiresOn(LocalDate.parse("2026-12-31"));
    rule.setMinimumSubtotal(new BigDecimal(minimum));
    if (maximum != null) rule.setMaxDiscount(new BigDecimal(maximum));
    return rule;
  }
}
