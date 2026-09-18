package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.OrderItem;
import tacos.Taco;

public class OrderPricingServiceTest {

  private final OrderPricingService service = new OrderPricingService(20, "MXN");

  @Test
  public void shouldCalculateDecimalPriceAndQuantityOnServer() {
    Ingredient first = ingredient("A", "1.005");
    Ingredient second = ingredient("B", "0.335");
    Taco taco = taco(first, second);

    OrderItem item = service.price(taco, 2);

    assertEquals(new BigDecimal("1.34"), item.getUnitPriceAtPurchase());
    assertEquals(new BigDecimal("2.68"), item.getSubtotal());
    assertEquals("MXN", service.getCurrency());
  }

  @Test
  public void shouldKeepPriceSnapshotAfterCatalogChanges() {
    Ingredient ingredient = ingredient("A", "2.50");
    OrderItem item = service.price(taco(ingredient), 3);

    ingredient.setUnitPrice(new BigDecimal("9.99"));

    assertEquals(new BigDecimal("2.50"), item.getUnitPriceAtPurchase());
    assertEquals(new BigDecimal("7.50"), item.getSubtotal());
  }

  @Test
  public void shouldRejectInvalidQuantities() {
    Taco taco = taco(ingredient("A", "1.00"));
    assertStatus(() -> service.price(taco, 0));
    assertStatus(() -> service.price(taco, 21));
  }

  private Ingredient ingredient(String id, String price) {
    return new Ingredient(id, "Ingredient " + id, Type.WRAP, new BigDecimal(price), true, 10, 2, 1L);
  }

  private Taco taco(Ingredient... ingredients) {
    Taco taco = new Taco();
    taco.setName("Test taco");
    taco.setIngredients(List.of(ingredients));
    return taco;
  }

  private void assertStatus(Runnable action) {
    try {
      action.run();
    } catch (ResponseStatusException exception) {
      assertEquals(HttpStatus.UNPROCESSABLE_ENTITY, exception.getStatus());
      return;
    }
    throw new AssertionError("Expected quantity rejection");
  }
}
