package tacos.data;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.SpringBootConfiguration;
import org.springframework.boot.autoconfigure.EnableAutoConfiguration;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.data.mongodb.core.ReactiveMongoTemplate;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;

@DataMongoTest(properties = {"spring.mongodb.embedded.version=4.0.12",
    "spring.data.mongodb.auto-index-creation=true"})
@EnabledIfSystemProperty(named = "tacocloud.mongo.integration", matches = "true")
public class KitchenQueueRepositoryMongoTest {
  @SpringBootConfiguration
  @EnableAutoConfiguration
  static class TestApplication {
  }

  @Autowired
  private OrderRepository orders;

  @Autowired
  private ReactiveMongoTemplate template;

  @BeforeEach
  public void clean() {
    template.remove(TacoOrder.class).all().block();
  }

  @Test
  public void shouldClaimOneOrderOnlyOnceUnderConcurrency() {
    template.insert(order("order-1", 1)).block();
    Date now = new Date();

    List<TacoOrder> claimed = Flux.merge(
        orders.claimNext("station-1", "cook-1", now, change("cook-1", now)),
        orders.claimNext("station-2", "cook-2", now, change("cook-2", now)))
        .collectList().block();

    assertEquals(1, claimed.size());
    assertEquals(OrderStatus.ACCEPTED, claimed.get(0).getStatus());
  }

  @Test
  public void shouldClaimOldestOrderWithStableIdTieBreak() {
    template.insertAll(List.of(order("order-b", 1), order("order-a", 1),
        order("order-old", 0))).then().block();
    Date now = new Date();

    TacoOrder claimed = orders.claimNext("station-1", "cook-1", now,
        change("cook-1", now)).block();

    assertEquals("order-old", claimed.getId());
  }

  @Test
  public void shouldPreventConcurrentClaimsByTheSameStation() {
    template.insertAll(List.of(order("order-1", 1), order("order-2", 2))).then().block();
    Date now = new Date();

    Mono.whenDelayError(
        orders.claimNext("station-1", "cook-1", now, change("cook-1", now)).then(),
        orders.claimNext("station-1", "cook-1", now, change("cook-1", now)).then())
        .onErrorResume(error -> Mono.empty()).block();

    assertEquals(1, orders.countByStatus(OrderStatus.ACCEPTED).block());
    assertEquals(1, orders.countByStatus(OrderStatus.CREATED).block());
  }

  private TacoOrder order(String id, long placedAt) {
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setPlacedAt(new Date(placedAt));
    order.setStatus(OrderStatus.CREATED);
    return order;
  }

  private OrderStatusChange change(String cookId, Date now) {
    return new OrderStatusChange(OrderStatus.CREATED, OrderStatus.ACCEPTED, now, cookId,
        "KITCHEN", "KITCHEN_CLAIM", "CLAIMED");
  }
}
