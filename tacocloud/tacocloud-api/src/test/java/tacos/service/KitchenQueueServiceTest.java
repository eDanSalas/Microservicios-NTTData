package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.OrderItem;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;

public class KitchenQueueServiceTest {
  private final OrderRepository orders = mock(OrderRepository.class);
  private final OrderWorkflowService workflow = mock(OrderWorkflowService.class);
  private final User cook = user("cook-1", "ROLE_KITCHEN");
  private KitchenQueueProperties properties;
  private KitchenQueueService service;

  @BeforeEach
  public void setUp() {
    properties = new KitchenQueueProperties();
    properties.setStationId("station-1");
    service = new KitchenQueueService(orders, workflow, properties,
        Clock.fixed(Instant.parse("2026-09-20T18:00:00Z"), ZoneOffset.UTC));
  }

  @Test
  public void shouldKeepRepositoryFifoAndIncreaseEtaWithQueue() {
    TacoOrder first = order("order-1", 1, 2);
    TacoOrder second = order("order-2", 2, 2);
    when(orders.findByStatusOrderByPlacedAtAscIdAsc(OrderStatus.CREATED))
        .thenReturn(Flux.just(first, second));

    StepVerifier.create(service.queue(cook)).assertNext(view -> {
      assertEquals("order-1", view.getOrder().getId());
      assertEquals(10, view.getEstimatedPrepMinutes());
    }).assertNext(view -> {
      assertEquals("order-2", view.getOrder().getId());
      assertEquals(17, view.getEstimatedPrepMinutes());
    }).verifyComplete();
  }

  @Test
  public void shouldAtomicallyClaimWithConfiguredStationAndAuthenticatedCook() {
    TacoOrder claimed = order("order-1", 1, 2);
    claimed.setStatus(OrderStatus.ACCEPTED);
    claimed.setStationId("station-1");
    claimed.setCookId("cook-1");
    when(orders.claimNext(eq("station-1"), eq("cook-1"), any(Date.class),
        any(OrderStatusChange.class))).thenReturn(Mono.just(claimed));

    StepVerifier.create(service.claimNext(cook)).assertNext(view -> {
      assertEquals(OrderStatus.ACCEPTED, view.getOrder().getStatus());
      assertEquals("station-1", view.getOrder().getStationId());
      assertEquals(10, view.getEstimatedPrepMinutes());
    }).verifyComplete();

    ArgumentCaptor<OrderStatusChange> change = ArgumentCaptor.forClass(OrderStatusChange.class);
    verify(orders).claimNext(eq("station-1"), eq("cook-1"), any(Date.class), change.capture());
    assertEquals("KITCHEN_CLAIM", change.getValue().getOrigin());
    assertEquals(Instant.parse("2026-09-20T18:00:00Z"),
        change.getValue().getChangedAt().toInstant());
  }

  @Test
  public void shouldReportEmptyQueueAndActiveStationConflict() {
    when(orders.claimNext(eq("station-1"), eq("cook-1"), any(Date.class), any()))
        .thenReturn(Mono.empty());
    assertStatus(service.claimNext(cook), HttpStatus.NOT_FOUND);
    when(orders.claimNext(eq("station-1"), eq("cook-1"), any(Date.class), any()))
        .thenReturn(Mono.error(new DuplicateKeyException("active station")));
    assertStatus(service.claimNext(cook), HttpStatus.CONFLICT);
  }

  @Test
  public void shouldRequireKitchenRole() {
    ResponseStatusException error = assertThrows(ResponseStatusException.class,
        () -> service.queue(user("admin-1", "ROLE_ADMIN")));
    assertEquals(HttpStatus.FORBIDDEN, error.getStatus());
  }

  @Test
  public void shouldUpdateStatusThroughKitchenWorkflow() {
    TacoOrder preparing = order("order-1", 1, 2);
    preparing.setStatus(OrderStatus.PREPARING);
    when(workflow.transition("order-1", OrderStatus.PREPARING, "STARTED", cook))
        .thenReturn(Mono.just(preparing));

    StepVerifier.create(service.updateStatus("order-1", OrderStatus.PREPARING, "STARTED", cook))
        .assertNext(view -> assertEquals(OrderStatus.PREPARING, view.getOrder().getStatus()))
        .verifyComplete();
  }

  private void assertStatus(Mono<KitchenOrderView> result, HttpStatus status) {
    StepVerifier.create(result).expectErrorMatches(error -> error instanceof ResponseStatusException
        && ((ResponseStatusException) error).getStatus() == status).verify();
  }

  private TacoOrder order(String id, int quantity, int ingredients) {
    Taco taco = new Taco();
    taco.setName("Kitchen taco");
    taco.setIngredients(java.util.stream.IntStream.range(0, ingredients)
        .mapToObj(index -> new Ingredient("I" + index, "Ingredient " + index,
            Ingredient.Type.PROTEIN)).toList());
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setPlacedAt(new Date());
    order.setItems(List.of(new OrderItem(taco, quantity, BigDecimal.ONE,
        BigDecimal.valueOf(quantity))));
    return order;
  }

  private static User user(String id, String role) {
    User user = mock(User.class);
    when(user.getId()).thenReturn(id);
    org.mockito.Mockito.doReturn(List.of(new SimpleGrantedAuthority(role)))
        .when(user).getAuthorities();
    return user;
  }
}
