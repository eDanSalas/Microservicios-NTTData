package tacos.service;

import java.time.Clock;
import java.util.Date;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.Map;
import java.util.Set;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.messaging.OrderEventType;
import tacos.messaging.OrderMessagingService;
import tacos.observability.CorrelationIds;
import tacos.observability.BusinessMetrics;

@Service
public class OrderWorkflowService {
  private static final Map<OrderStatus, Set<OrderStatus>> TRANSITIONS = transitions();
  private final OrderRepository repo;
  private final InventoryService inventoryService;
  private final OrderMessagingService orderMessages;
  private final OrderEventFactory eventFactory;
  private final Clock clock;
  private final BusinessMetrics metrics;

  public OrderWorkflowService(OrderRepository repo, InventoryService inventoryService,
      OrderMessagingService orderMessages, OrderEventFactory eventFactory,
      @Qualifier("orderWorkflowClock") Clock clock, BusinessMetrics metrics) {
    this.repo = repo;
    this.inventoryService = inventoryService;
    this.orderMessages = orderMessages;
    this.eventFactory = eventFactory;
    this.clock = clock;
    this.metrics = metrics;
  }

  public Mono<TacoOrder> transition(String orderId, OrderStatus target, String reason, User actor) {
    if (!isOperator(actor)) return Mono.error(new ResponseStatusException(HttpStatus.FORBIDDEN,
        "The authenticated role cannot change order status"));
    return repo.findById(orderId).switchIfEmpty(notFound()).flatMap(order -> {
      if (order.getStatus() == target) return Mono.just(order);
      if (!canExecute(actor, target)) return Mono.error(new ResponseStatusException(
          HttpStatus.FORBIDDEN, "The authenticated role cannot perform this transition"));
      return change(order, target, reason, actor, "API_STATUS");
    });
  }

  public Mono<TacoOrder> cancel(String orderId, String reason, User owner) {
    String userId = userId(owner);
    return repo.findByIdAndUserId(orderId, userId).switchIfEmpty(notFound()).flatMap(order -> {
      if (order.getStatus() == OrderStatus.CANCELLED)
        return inventoryService.release(order.getId()).thenReturn(order);
      return change(order, OrderStatus.CANCELLED, reason, owner, "API_CANCEL");
    });
  }

  public boolean isAllowed(OrderStatus from, OrderStatus to) {
    return from != null && TRANSITIONS.getOrDefault(from, Set.of()).contains(to);
  }

  private Mono<TacoOrder> change(TacoOrder order, OrderStatus target, String reason, User actor,
      String origin) {
    OrderStatus previous = order.getStatus();
    if (!isAllowed(previous, target)) return Mono.error(new ResponseStatusException(
        HttpStatus.CONFLICT, "Transition from " + previous + " to " + target + " is not allowed"));
    order.setStatus(target);
    if (target == OrderStatus.READY || target == OrderStatus.CANCELLED)
      order.setStationActive(false);
    if (order.getStatusHistory() == null) order.setStatusHistory(new ArrayList<>());
    order.getStatusHistory().add(new OrderStatusChange(previous, target,
        Date.from(clock.instant()), actor.getId(), role(actor), origin, reason(reason, target)));
    return repo.save(order).onErrorMap(OptimisticLockingFailureException.class,
        error -> new ResponseStatusException(HttpStatus.CONFLICT,
            "Order was updated concurrently", error)).flatMap(saved -> {
              Mono<TacoOrder> persisted = target == OrderStatus.CANCELLED
                  ? inventoryService.release(saved.getId()).thenReturn(saved) : Mono.just(saved);
              OrderEventType type = target == OrderStatus.CANCELLED
                  ? OrderEventType.CANCELLED : OrderEventType.STATUS_CHANGED;
              return persisted.flatMap(result -> Mono.deferContextual(context -> Mono.fromRunnable(
                  () -> orderMessages.sendOrder(eventFactory.event(type, result,
                      CorrelationIds.current(context)))).thenReturn(result)))
                  .doOnSuccess(result -> {
                    if (target == OrderStatus.CANCELLED) metrics.cancelled("workflow");
                  });
            });
  }

  private boolean canExecute(User actor, OrderStatus target) {
    if (hasRole(actor, "ROLE_ADMIN")) return true;
    return hasRole(actor, "ROLE_KITCHEN") && Set.of(OrderStatus.ACCEPTED,
        OrderStatus.PREPARING, OrderStatus.READY).contains(target);
  }

  private boolean isOperator(User actor) {
    return hasRole(actor, "ROLE_ADMIN") || hasRole(actor, "ROLE_KITCHEN");
  }

  private boolean hasRole(User user, String role) {
    return user != null && user.getAuthorities() != null && user.getAuthorities().stream()
        .anyMatch(authority -> role.equals(authority.getAuthority()));
  }

  private String role(User actor) {
    if (hasRole(actor, "ROLE_ADMIN")) return "ADMIN";
    if (hasRole(actor, "ROLE_KITCHEN")) return "KITCHEN";
    return "USER";
  }

  private String userId(User user) {
    if (user == null || user.getId() == null || user.getId().isBlank()) throw new ResponseStatusException(
        HttpStatus.UNAUTHORIZED, "Authentication is required");
    return user.getId();
  }

  private String reason(String value, OrderStatus target) {
    return value == null || value.isBlank()
        ? target == OrderStatus.CANCELLED ? "USER_CANCELLED" : "STATUS_UPDATED" : value.trim();
  }

  private <T> Mono<T> notFound() {
    return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND, "Order not found"));
  }

  private static Map<OrderStatus, Set<OrderStatus>> transitions() {
    Map<OrderStatus, Set<OrderStatus>> values = new EnumMap<>(OrderStatus.class);
    values.put(OrderStatus.CREATED, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
    values.put(OrderStatus.PLACED, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
    values.put(OrderStatus.ACCEPTED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
    values.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY));
    values.put(OrderStatus.READY, Set.of(OrderStatus.OUT_FOR_DELIVERY));
    values.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED));
    return values;
  }
}
