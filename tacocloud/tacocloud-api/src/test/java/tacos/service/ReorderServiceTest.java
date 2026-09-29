package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.OrderItem;
import tacos.OrderStatus;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.OrderRepository;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.ReorderRequest;

public class ReorderServiceTest {
  private final OrderHistoryService history = mock(OrderHistoryService.class);
  private final OrderCreationService creation = mock(OrderCreationService.class);
  private final OrderRepository orders = mock(OrderRepository.class);
  private final User user = mock(User.class);
  private ReorderService service;
  private TacoOrder source;

  @BeforeEach
  public void setUp() {
    service = new ReorderService(history, creation, orders);
    source = order("original", "10.00", "20.00", OrderStatus.DELIVERED);
    when(user.getId()).thenReturn("user-1");
    when(history.findMine(user, "original")).thenReturn(Mono.just(source));
    when(orders.findByIdAndUserId(anyString(), anyString())).thenReturn(Mono.empty());
  }

  @Test
  public void shouldQuoteCurrentPriceBeforeCreating() {
    TacoOrder current = order(null, "12.00", "24.00", OrderStatus.PLACED);
    when(creation.quote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user)))
        .thenReturn(Mono.just(current));

    StepVerifier.create(service.reorder(user, "original", request(false), "retry-1"))
        .assertNext(result -> {
          assertEquals("QUOTE", result.getStatus());
          assertEquals(new BigDecimal("20.00"), result.getPreviousTotal());
          assertEquals(new BigDecimal("24.00"), result.getCurrentTotal());
          assertEquals(2, result.getDifferences().size());
        }).verifyComplete();

    verify(creation, never()).create(org.mockito.ArgumentMatchers.any(),
        org.mockito.ArgumentMatchers.any(), anyString());
    assertEquals("original", source.getId());
    assertEquals(OrderStatus.DELIVERED, source.getStatus());
  }

  @Test
  public void shouldCreateIndependentOrderAfterConfirmation() {
    Date originalDate = source.getPlacedAt();
    TacoOrder quote = order(null, "12.00", "24.00", OrderStatus.PLACED);
    when(creation.quote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user)))
        .thenReturn(Mono.just(quote));
    when(creation.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user),
        anyString())).thenAnswer(invocation -> {
          TacoOrder created = order(invocation.getArgument(2), "12.00", "24.00",
              OrderStatus.PLACED);
          created.setPlacedAt(new Date(originalDate.getTime() + 1000));
          return Mono.just(created);
        });

    StepVerifier.create(service.reorder(user, "original", request(true), "retry-1"))
        .assertNext(result -> {
          assertEquals("CREATED", result.getStatus());
          assertNotEquals(source.getId(), result.getOrder().getId());
          assertEquals(OrderStatus.PLACED, result.getOrder().getStatus());
          assertNotEquals(originalDate, result.getOrder().getPlacedAt());
        }).verifyComplete();

    ArgumentCaptor<OrderCreateRequest> command = ArgumentCaptor.forClass(OrderCreateRequest.class);
    verify(creation).create(command.capture(), org.mockito.ArgumentMatchers.eq(user), anyString());
    assertEquals("payment-2", command.getValue().getPaymentMethodId());
    assertEquals("coupon-1", command.getValue().getCouponCode());
    assertEquals(List.of("INGR"), command.getValue().getItems().get(0).getTaco().getIngredientIds());
    assertEquals("original", source.getId());
  }

  @Test
  public void shouldReturnPreviouslyCreatedOrderOnRetry() {
    TacoOrder created = order("created", "10.00", "20.00", OrderStatus.PLACED);
    when(orders.findByIdAndUserId(anyString(), org.mockito.ArgumentMatchers.eq("user-1")))
        .thenReturn(Mono.just(created));

    StepVerifier.create(service.reorder(user, "original", request(true), "retry-1"))
        .assertNext(result -> assertSame(created, result.getOrder())).verifyComplete();

    verifyNoInteractions(creation);
  }

  @Test
  public void shouldPropagateUnavailableInventoryWithoutChangingOriginal() {
    TacoOrder quote = order(null, "10.00", "20.00", OrderStatus.PLACED);
    when(creation.quote(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user)))
        .thenReturn(Mono.just(quote));
    ResponseStatusException unavailable = new ResponseStatusException(HttpStatus.CONFLICT,
        "Ingredient is out of stock");
    when(creation.create(org.mockito.ArgumentMatchers.any(), org.mockito.ArgumentMatchers.eq(user),
        anyString())).thenReturn(Mono.error(unavailable));

    StepVerifier.create(service.reorder(user, "original", request(false), "retry-1"))
        .expectErrorMatches(error -> error == unavailable).verify();

    assertEquals("original", source.getId());
    assertEquals(OrderStatus.DELIVERED, source.getStatus());
  }

  @Test
  public void shouldRejectForeignOrder() {
    ResponseStatusException missing = new ResponseStatusException(HttpStatus.NOT_FOUND,
        "Order not found");
    when(history.findMine(user, "foreign")).thenReturn(Mono.error(missing));

    StepVerifier.create(service.reorder(user, "foreign", request(false), "retry-1"))
        .expectErrorMatches(error -> error == missing).verify();

    verifyNoInteractions(creation);
    verify(orders, never()).findByIdAndUserId(anyString(), anyString());
  }

  private ReorderRequest request(boolean confirm) {
    ReorderRequest request = new ReorderRequest();
    request.setPaymentMethodId("payment-2");
    request.setConfirmPriceChange(confirm);
    return request;
  }

  private TacoOrder order(String id, String unitPrice, String total, OrderStatus status) {
    Ingredient ingredient = new Ingredient("INGR", "Ingredient", Type.PROTEIN);
    Taco taco = new Taco();
    taco.setName("Test taco");
    taco.setIngredients(List.of(ingredient));
    TacoOrder order = new TacoOrder();
    order.setId(id);
    order.setStatus(status);
    order.setDeliveryName("Test User");
    order.setDeliveryStreet("One Street");
    order.setDeliveryCity("City");
    order.setDeliveryState("State");
    order.setDeliveryZip("12345");
    order.setCouponCode("coupon-1");
    order.setItems(List.of(new OrderItem(taco, 2, new BigDecimal(unitPrice),
        new BigDecimal(total))));
    order.setSubtotal(new BigDecimal(total));
    order.setTotal(new BigDecimal(total));
    order.setCurrency("MXN");
    return order;
  }
}
