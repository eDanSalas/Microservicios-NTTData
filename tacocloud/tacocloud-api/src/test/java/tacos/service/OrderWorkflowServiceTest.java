package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.data.annotation.Version;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.inventory.InventoryService;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventType;
import tacos.messaging.OrderMessagingService;
import tacos.observability.BusinessMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;

public class OrderWorkflowServiceTest {
  private final OrderRepository orders = mock(OrderRepository.class);
  private final InventoryService inventory = mock(InventoryService.class);
  private final OrderMessagingService orderMessages = mock(OrderMessagingService.class);
  private final OrderEventFactory eventFactory = mock(OrderEventFactory.class);
  private final OrderEvent statusEvent = event(OrderEventType.STATUS_CHANGED);
  private final OrderEvent cancelledEvent = event(OrderEventType.CANCELLED);
  private final Clock clock = Clock.fixed(Instant.parse("2026-09-20T18:00:00Z"), ZoneOffset.UTC);
  private final User kitchen = user("cook-1", "ROLE_KITCHEN");
  private final User admin = user("admin-1", "ROLE_ADMIN");
  private final User owner = user("owner-1", "ROLE_USER");
  private OrderWorkflowService service;

  @BeforeEach
  public void setUp() {
    service = new OrderWorkflowService(orders, inventory, orderMessages, eventFactory, clock,
        new BusinessMetrics(new SimpleMeterRegistry()));
    when(inventory.release(any())).thenReturn(Mono.empty());
    when(eventFactory.event(org.mockito.Mockito.eq(OrderEventType.STATUS_CHANGED), any(),
        org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(statusEvent);
    when(eventFactory.event(org.mockito.Mockito.eq(OrderEventType.CANCELLED), any(),
        org.mockito.ArgumentMatchers.anyString()))
        .thenReturn(cancelledEvent);
  }

  @Test
  public void shouldDefineTheCompleteTransitionMatrix() {
    Map<OrderStatus, Set<OrderStatus>> expected = new EnumMap<>(OrderStatus.class);
    expected.put(OrderStatus.CREATED, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
    expected.put(OrderStatus.PLACED, Set.of(OrderStatus.ACCEPTED, OrderStatus.CANCELLED));
    expected.put(OrderStatus.ACCEPTED, Set.of(OrderStatus.PREPARING, OrderStatus.CANCELLED));
    expected.put(OrderStatus.PREPARING, Set.of(OrderStatus.READY));
    expected.put(OrderStatus.READY, Set.of(OrderStatus.OUT_FOR_DELIVERY));
    expected.put(OrderStatus.OUT_FOR_DELIVERY, Set.of(OrderStatus.DELIVERED));

    for (OrderStatus from : OrderStatus.values())
      for (OrderStatus to : OrderStatus.values())
        assertEquals(expected.getOrDefault(from, Set.of()).contains(to),
            service.isAllowed(from, to), from + " -> " + to);
  }

  @Test
  public void shouldAdvanceAndAuditKitchenTransition() {
    TacoOrder order = order(OrderStatus.CREATED);
    when(orders.findById("order-1")).thenReturn(Mono.just(order));
    when(orders.save(order)).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("order-1", OrderStatus.ACCEPTED, "Accepted", kitchen))
        .assertNext(updated -> {
          assertEquals(OrderStatus.ACCEPTED, updated.getStatus());
          assertEquals(1, updated.getStatusHistory().size());
          assertEquals("cook-1", updated.getStatusHistory().get(0).getActorId());
          assertEquals("KITCHEN", updated.getStatusHistory().get(0).getActorRole());
          assertEquals("API_STATUS", updated.getStatusHistory().get(0).getOrigin());
          assertEquals(Instant.parse("2026-09-20T18:00:00Z"),
              updated.getStatusHistory().get(0).getChangedAt().toInstant());
        }).verifyComplete();

    verify(orderMessages).sendOrder(statusEvent);
  }

  @Test
  public void shouldRejectInvalidJump() {
    TacoOrder order = order(OrderStatus.CREATED);
    when(orders.findById("order-1")).thenReturn(Mono.just(order));

    assertConflict(service.transition("order-1", OrderStatus.DELIVERED, null, admin));

    verify(orders, never()).save(any());
  }

  @Test
  public void shouldEnforceRoleForStatusTransitions() {
    assertStatus(service.transition("order-1", OrderStatus.ACCEPTED, null, owner),
        HttpStatus.FORBIDDEN);
    verifyNoInteractions(orders);
  }

  @Test
  public void shouldCancelOwnedOrderAndReleaseInventory() {
    TacoOrder order = order(OrderStatus.ACCEPTED);
    order.setUserId("owner-1");
    when(orders.findByIdAndUserId("order-1", "owner-1")).thenReturn(Mono.just(order));
    when(orders.save(order)).thenReturn(Mono.just(order));

    StepVerifier.create(service.cancel("order-1", "Changed plans", owner))
        .assertNext(updated -> {
          assertEquals(OrderStatus.CANCELLED, updated.getStatus());
          assertEquals("Changed plans", updated.getStatusHistory().get(0).getReason());
        }).verifyComplete();

    verify(inventory).release("order-1");
    verify(orderMessages).sendOrder(cancelledEvent);
  }

  @Test
  public void shouldHideForeignOrderAndRejectLateCancellation() {
    when(orders.findByIdAndUserId("foreign", "owner-1")).thenReturn(Mono.empty());
    assertStatus(service.cancel("foreign", null, owner), HttpStatus.NOT_FOUND);
    TacoOrder preparing = order(OrderStatus.PREPARING);
    when(orders.findByIdAndUserId("order-1", "owner-1")).thenReturn(Mono.just(preparing));
    assertConflict(service.cancel("order-1", null, owner));
  }

  @Test
  public void shouldReturnRepeatedTransitionWithoutDuplicateAudit() {
    TacoOrder order = order(OrderStatus.ACCEPTED);
    when(orders.findById("order-1")).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("order-1", OrderStatus.ACCEPTED, null, kitchen))
        .assertNext(updated -> assertTrue(updated.getStatusHistory().isEmpty())).verifyComplete();

    verify(orders, never()).save(any());
  }

  @Test
  public void shouldReleaseStationWhenOrderBecomesReady() {
    TacoOrder order = order(OrderStatus.PREPARING);
    order.setStationActive(true);
    when(orders.findById("order-1")).thenReturn(Mono.just(order));
    when(orders.save(order)).thenReturn(Mono.just(order));

    StepVerifier.create(service.transition("order-1", OrderStatus.READY, null, kitchen))
        .assertNext(updated -> assertEquals(false, updated.isStationActive())).verifyComplete();
  }

  @Test
  public void shouldTranslateOptimisticLockConflict() {
    TacoOrder order = order(OrderStatus.CREATED);
    when(orders.findById("order-1")).thenReturn(Mono.just(order));
    when(orders.save(order)).thenReturn(Mono.error(new OptimisticLockingFailureException("stale")));

    assertConflict(service.transition("order-1", OrderStatus.ACCEPTED, null, kitchen));
  }

  @Test
  public void shouldVersionOrdersForConcurrentUpdates() throws Exception {
    assertTrue(TacoOrder.class.getDeclaredField("version").isAnnotationPresent(Version.class));
  }

  private void assertConflict(Mono<TacoOrder> result) {
    assertStatus(result, HttpStatus.CONFLICT);
  }

  private void assertStatus(Mono<TacoOrder> result, HttpStatus status) {
    StepVerifier.create(result).expectErrorMatches(error -> error instanceof ResponseStatusException
        && ((ResponseStatusException) error).getStatus() == status).verify();
  }

  private TacoOrder order(OrderStatus status) {
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setStatus(status);
    return order;
  }

  private static User user(String id, String role) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority(role)))
        .when(user).getAuthorities();
    return user;
  }

  private static OrderEvent event(OrderEventType type) {
    return new OrderEvent("123e4567-e89b-12d3-a456-426614174000", type, 1,
        "2026-09-21T06:00:00Z", "123e4567-e89b-12d3-a456-426614174001",
        new tacos.messaging.OrderEventPayload("order-1", "CREATED", null, null, List.of()));
  }
}
