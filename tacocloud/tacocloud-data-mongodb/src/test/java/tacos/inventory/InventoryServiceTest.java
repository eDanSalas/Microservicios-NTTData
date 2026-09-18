package tacos.inventory;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import org.bson.Document;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.data.mongodb.core.FindAndModifyOptions;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.mongodb.core.query.Update;

import com.mongodb.client.result.UpdateResult;

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

public class InventoryServiceTest {

  private final Map<String, AtomicInteger> stock = new ConcurrentHashMap<>();
  private final Map<String, AtomicBoolean> available = new ConcurrentHashMap<>();
  private final Map<String, InventoryReservation> reservations = new ConcurrentHashMap<>();
  private final List<String> reserveOrder = Collections.synchronizedList(new ArrayList<>());
  private InventoryService service;

  @BeforeEach
  public void setUp() {
    ReactiveMongoTemplate template = Mockito.mock(ReactiveMongoTemplate.class);
    when(template.findById(anyString(), eq(InventoryReservation.class))).thenAnswer(invocation ->
        Mono.defer(() -> Mono.justOrEmpty(reservations.get(invocation.getArgument(0, String.class)))));
    when(template.insert(any(InventoryReservation.class))).thenAnswer(invocation -> Mono.defer(() -> {
      InventoryReservation reservation = invocation.getArgument(0, InventoryReservation.class);
      reservations.put(reservation.getId(), reservation);
      return Mono.just(reservation);
    }));
    when(template.save(any(InventoryReservation.class))).thenAnswer(invocation -> Mono.defer(() -> {
      InventoryReservation reservation = invocation.getArgument(0, InventoryReservation.class);
      reservations.put(reservation.getId(), reservation);
      return Mono.just(reservation);
    }));
    when(template.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
        eq(Ingredient.class))).thenAnswer(invocation -> reserveIngredient(invocation.getArgument(0, Query.class),
            invocation.getArgument(1, Update.class)));
    when(template.findAndModify(any(Query.class), any(Update.class), any(FindAndModifyOptions.class),
        eq(InventoryReservation.class))).thenAnswer(invocation -> claimRelease(invocation.getArgument(0, Query.class)));
    when(template.updateFirst(any(Query.class), any(Update.class), eq(Ingredient.class))).thenAnswer(invocation ->
        restoreOrUpdate(invocation.getArgument(0, Query.class), invocation.getArgument(1, Update.class)));
    service = new InventoryService(template);
  }

  @Test
  public void shouldAllowOnlyOneConcurrentBuyerForLastUnit() {
    ingredient("A", 1);
    Mono<Boolean> first = result(service.reserve(order("order-1", 1, "A"), "reservation-1"));
    Mono<Boolean> second = result(service.reserve(order("order-2", 1, "A"), "reservation-2"));
    List<Boolean> results = Flux.merge(first.subscribeOn(Schedulers.parallel()),
        second.subscribeOn(Schedulers.parallel())).collectList().block();
    assertEquals(1, results.stream().filter(Boolean::booleanValue).count());
    assertEquals(0, stock.get("A").get());
  }

  @Test
  public void shouldCompensatePartialReservationInStableOrder() {
    ingredient("A", 1);
    ingredient("B", 0);
    StepVerifier.create(service.reserve(order("order-1", 1, "B", "A"), "reservation-1"))
        .expectError(InsufficientStockException.class).verify();
    assertEquals(List.of("A", "B"), reserveOrder);
    assertEquals(1, stock.get("A").get());
    assertEquals(0, stock.get("B").get());
    assertEquals(InventoryReservationStatus.COMPENSATED, reservations.get("reservation-1").getStatus());
  }

  @Test
  public void shouldNotReserveTwiceForSameKey() {
    ingredient("A", 2);
    TacoOrder order = order("order-1", 1, "A");
    service.reserve(order, "reservation-1").then(service.reserve(order, "reservation-1")).block();
    assertEquals(1, stock.get("A").get());
  }

  @Test
  public void shouldReleaseExactlyOnce() {
    ingredient("A", 2);
    TacoOrder order = order("order-1", 1, "A");
    service.reserve(order, "reservation-1").then(service.release("reservation-1"))
        .then(service.release("reservation-1")).block();
    assertEquals(2, stock.get("A").get());
    assertTrue(available.get("A").get());
    assertEquals(InventoryReservationStatus.RELEASED, reservations.get("reservation-1").getStatus());
  }

  private Mono<Boolean> result(Mono<InventoryReservation> reservation) {
    return reservation.map(value -> true).onErrorReturn(false);
  }

  private Mono<Ingredient> reserveIngredient(Query query, Update update) {
    return Mono.defer(() -> {
      String id = query.getQueryObject().getString("_id");
      int requested = -increment(update);
      reserveOrder.add(id);
      AtomicInteger current = stock.get(id);
      while (available.get(id).get() && current.get() >= requested) {
        int value = current.get();
        if (current.compareAndSet(value, value - requested)) return Mono.just(domainIngredient(id));
      }
      return Mono.empty();
    });
  }

  private Mono<InventoryReservation> claimRelease(Query query) {
    return Mono.defer(() -> {
      InventoryReservation reservation = reservations.get(query.getQueryObject().getString("_id"));
      if (reservation == null || reservation.getStatus() != InventoryReservationStatus.RESERVED) return Mono.empty();
      reservation.setStatus(InventoryReservationStatus.RELEASING);
      return Mono.just(reservation);
    });
  }

  private Mono<UpdateResult> restoreOrUpdate(Query query, Update update) {
    return Mono.defer(() -> {
      String id = query.getQueryObject().getString("_id");
      int increment = increment(update);
      if (increment > 0) stock.get(id).addAndGet(increment);
      Document set = update.getUpdateObject().get("$set", Document.class);
      if (set != null && set.containsKey("available")) available.get(id).set(set.getBoolean("available"));
      return Mono.just(UpdateResult.acknowledged(1, 1L, null));
    });
  }

  private int increment(Update update) {
    Document increment = update.getUpdateObject().get("$inc", Document.class);
    return increment == null ? 0 : ((Number) increment.get("stockOnHand")).intValue();
  }

  private void ingredient(String id, int quantity) {
    stock.put(id, new AtomicInteger(quantity));
    available.put(id, new AtomicBoolean(quantity > 0));
  }

  private Ingredient domainIngredient(String id) {
    return new Ingredient(id, id, Type.VEGGIES, BigDecimal.ONE, available.get(id).get(), stock.get(id).get(), 0, 1L);
  }

  private TacoOrder order(String id, int quantity, String... ingredientIds) {
    Taco taco = new Taco();
    taco.setIngredients(List.of(ingredientIds).stream()
        .map(ingredientId -> new Ingredient(ingredientId, ingredientId, Type.VEGGIES)).toList());
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setItems(List.of(new OrderItem(taco, quantity, BigDecimal.ONE, BigDecimal.ONE)));
    return order;
  }
}
