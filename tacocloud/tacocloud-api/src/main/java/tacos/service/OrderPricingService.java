package tacos.service;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import tacos.Ingredient;
import tacos.OrderItem;
import tacos.Taco;

@Service
public class OrderPricingService {

  private final int maxItemQuantity;
  private final String currency;

  public OrderPricingService(@Value("${tacocloud.order.max-item-quantity:20}") int maxItemQuantity,
      @Value("${tacocloud.order.currency:MXN}") String currency) {
    this.maxItemQuantity = maxItemQuantity;
    this.currency = currency;
  }

  public OrderItem price(Taco taco, int quantity) {
    if (quantity < 1 || quantity > maxItemQuantity)
      throw businessError("Quantity must be between 1 and " + maxItemQuantity);
    BigDecimal unitPrice = taco.getIngredients().stream().map(this::priceOf)
        .reduce(BigDecimal.ZERO, BigDecimal::add).setScale(2, RoundingMode.HALF_UP);
    BigDecimal subtotal = unitPrice.multiply(BigDecimal.valueOf(quantity)).setScale(2, RoundingMode.HALF_UP);
    return new OrderItem(taco, quantity, unitPrice, subtotal);
  }

  public String getCurrency() {
    return currency;
  }

  private BigDecimal priceOf(Ingredient ingredient) {
    if (!ingredient.isAvailable() || ingredient.getUnitPrice() == null)
      throw businessError("Ingredient is not available: " + ingredient.getId());
    return ingredient.getUnitPrice();
  }

  private ResponseStatusException businessError(String detail) {
    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }
}
