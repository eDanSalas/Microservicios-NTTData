package tacos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.context.annotation.Import;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.InventoryReservation;
import tacos.InventoryReservationStatus;
import tacos.OrderItem;
import tacos.Taco;
import tacos.TacoOrder;

@DataMongoTest(properties = "spring.mongodb.embedded.version=4.0.28")
@Import(InventoryService.class)
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class InventoryServiceMongoTest {

  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private ReactiveMongoTemplate template;

  @Autowired
  private InventoryService service;

  @BeforeEach
  public void clean() {
    template.remove(Ingredient.class).all().then(template.remove(InventoryReservation.class).all()).block();
  }

  @Test
  public void shouldAllowOnlyOneConcurrentBuyerForLastUnit() {
    saveIngredient("A", 1);
    Mono<Boolean> first = service.reserve(order("order-1", 1, "A"), "reservation-1")
        .map(reservation -> true).onErrorReturn(false).subscribeOn(Schedulers.parallel());
    Mono<Boolean> second = service.reserve(order("order-2", 1, "A"), "reservation-2")
        .map(reservation -> true).onErrorReturn(false).subscribeOn(Schedulers.parallel());
    List<Boolean> results = Flux.merge(first, second).collectList().block();
    assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    assertEquals(0, ingredient("A").getStockOnHand());
  }

  @Test
  public void shouldCompensatePartialReservationInStableOrder() {
    saveIngredient("A", 1);
    saveIngredient("B", 0);
    StepVerifier.create(service.reserve(order("order-1", 1, "B", "A"), "reservation-1"))
        .expectError(InsufficientStockException.class).verify();
    assertEquals(1, ingredient("A").getStockOnHand());
    assertEquals(0, ingredient("B").getStockOnHand());
    assertEquals(InventoryReservationStatus.COMPENSATED,
        template.findById("reservation-1", InventoryReservation.class).block().getStatus());
  }

  @Test
  public void shouldNotReserveTwiceForSameIdempotencyKey() {
    saveIngredient("A", 2);
    TacoOrder order = order("order-1", 1, "A");
    service.reserve(order, "reservation-1").then(service.reserve(order, "reservation-1")).block();
    assertEquals(1, ingredient("A").getStockOnHand());
  }

  @Test
  public void shouldReleaseExactlyOnce() {
    saveIngredient("A", 2);
    TacoOrder order = order("order-1", 1, "A");
    service.reserve(order, "reservation-1").then(service.release("reservation-1"))
        .then(service.release("reservation-1")).block();
    assertEquals(2, ingredient("A").getStockOnHand());
    assertTrue(ingredient("A").isAvailable());
    assertEquals(InventoryReservationStatus.RELEASED,
        template.findById("reservation-1", InventoryReservation.class).block().getStatus());
  }

  private void saveIngredient(String id, int stock) {
    template.save(new Ingredient(id, id, Type.VEGGIES, new BigDecimal("1.00"), stock > 0, stock, 0, null)).block();
  }

  private Ingredient ingredient(String id) {
    return template.findById(id, Ingredient.class).block();
  }

  private TacoOrder order(String id, int quantity, String... ingredientIds) {
    Taco taco = new Taco();
    taco.setName("Test taco");
    taco.setIngredients(List.of(ingredientIds).stream()
        .map(ingredientId -> new Ingredient(ingredientId, ingredientId, Type.VEGGIES)).toList());
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setItems(List.of(new OrderItem(taco, quantity, BigDecimal.ONE, BigDecimal.ONE)));
    return order;
  }
}
