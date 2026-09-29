package tacos.service;

import java.time.Clock;
import java.util.Date;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderItem;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;

@Service
public class KitchenQueueService {
  private final OrderRepository orders;
  private final OrderWorkflowService workflow;
  private final KitchenQueueProperties properties;
  private final Clock clock;

  public KitchenQueueService(OrderRepository orders, OrderWorkflowService workflow,
      KitchenQueueProperties properties, @Qualifier("orderWorkflowClock") Clock clock) {
    this.orders = orders;
    this.workflow = workflow;
    this.properties = properties;
    this.clock = clock;
  }

  public Flux<KitchenOrderView> queue(User cook) {
    requireKitchen(cook);
    return orders.findByStatusOrderByPlacedAtAscIdAsc(OrderStatus.CREATED).index()
        .map(indexed -> view(indexed.getT2(), Math.toIntExact(indexed.getT1())));
  }

  public Mono<KitchenOrderView> claimNext(User cook) {
    requireKitchen(cook);
    Date acceptedAt = Date.from(clock.instant());
    OrderStatusChange change = new OrderStatusChange(OrderStatus.CREATED, OrderStatus.ACCEPTED,
        acceptedAt, cook.getId(), "KITCHEN", "KITCHEN_CLAIM", "CLAIMED");
    return orders.claimNext(properties.getStationId(), cook.getId(), acceptedAt, change)
        .map(order -> view(order, 0)).switchIfEmpty(Mono.error(new ResponseStatusException(
            HttpStatus.NOT_FOUND, "Kitchen queue is empty")))
        .onErrorMap(DuplicateKeyException.class, error -> new ResponseStatusException(
            HttpStatus.CONFLICT, "Kitchen station already has an active order", error));
  }

  public Mono<KitchenOrderView> updateStatus(String orderId, OrderStatus status, String reason,
      User cook) {
    requireKitchen(cook);
    return workflow.transition(orderId, status, reason, cook).map(order -> view(order, 0));
  }

  int estimate(TacoOrder order, int queuedBefore) {
    int quantity = order.getItems() == null ? 0
        : order.getItems().stream().mapToInt(OrderItem::getQuantity).sum();
    int complexity = order.getItems() == null ? 0 : order.getItems().stream()
        .mapToInt(item -> item.getTaco() == null || item.getTaco().getIngredients() == null ? 0
            : item.getTaco().getIngredients().size() * item.getQuantity()).sum();
    return Math.addExact(properties.getBaseMinutes(), Math.addExact(
        Math.multiplyExact(queuedBefore, properties.getQueuedOrderMinutes()), Math.addExact(
            Math.multiplyExact(quantity, properties.getItemMinutes()),
            Math.multiplyExact(complexity, properties.getIngredientMinutes()))));
  }

  private KitchenOrderView view(TacoOrder order, int queuedBefore) {
    return new KitchenOrderView(order, estimate(order, queuedBefore));
  }

  private void requireKitchen(User user) {
    boolean kitchen = user != null && user.getId() != null && user.getAuthorities() != null
        && user.getAuthorities().stream()
            .anyMatch(authority -> "ROLE_KITCHEN".equals(authority.getAuthority()));
    if (!kitchen) throw new ResponseStatusException(HttpStatus.FORBIDDEN,
        "Kitchen role is required");
  }
}
