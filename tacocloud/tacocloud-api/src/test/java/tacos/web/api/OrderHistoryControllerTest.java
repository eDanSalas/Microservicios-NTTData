package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.OrderStatusChange;
import tacos.TacoOrder;
import tacos.User;
import tacos.service.OrderHistoryPage;
import tacos.service.OrderHistoryService;
import tacos.service.TacoClassificationService;
import tacos.web.api.dto.OrderDetailResponse;
import tacos.web.api.dto.OrderHistoryPageResponse;
import tacos.web.api.dto.OrderSummaryResponse;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.OrderHistoryMapper;
import tacos.web.api.mapper.OrderMapper;

public class OrderHistoryControllerTest {
  private OrderHistoryService service;
  private OrderHistoryMapper mapper;
  private User user;

  @BeforeEach
  public void setUp() {
    service = Mockito.mock(OrderHistoryService.class);
    mapper = new OrderHistoryMapper(new OrderMapper(new IngredientMapper(),
        new TacoClassificationService()));
    user = Mockito.mock(User.class);
  }

  @Test
  public void shouldReturnStablePageContract() {
    TacoOrder order = order();
    when(service.findMine(user, 1, 10))
        .thenReturn(Mono.just(new OrderHistoryPage(List.of(order), 1, 10, 21, 3)));

    OrderHistoryPageResponse<OrderSummaryResponse> result =
        new UserOrderHistoryController(service, mapper).findAll(user, 1, 10).block();

    assertEquals(1, result.getPage());
    assertEquals(3, result.getTotalPages());
    assertEquals("order-1", result.getContent().get(0).getId());
  }

  @Test
  public void shouldReturnDetailWithoutSensitiveFields() throws Exception {
    TacoOrder order = order();
    order.setPaymentMethodId("payment-method");
    order.setPaymentBrand("VISA");
    order.setPaymentLast4("4242");
    when(service.findMine(user, "order-1")).thenReturn(Mono.just(order));

    OrderDetailResponse result =
        new UserOrderHistoryController(service, mapper).findOne(user, "order-1").block();
    JsonNode json = new ObjectMapper().valueToTree(result);

    assertEquals("4242", json.get("paymentMethod").get("last4").asText());
    assertFalse(json.has("paymentMethodId"));
    assertFalse(json.has("paymentToken"));
    assertFalse(json.has("user"));
    assertFalse(json.has("userId"));
  }

  @Test
  public void shouldReturnOrderedSafeStatusHistory() {
    TacoOrder order = order();
    order.setStatusHistory(List.of(
        new OrderStatusChange(OrderStatus.CREATED, OrderStatus.ACCEPTED, new Date(2000),
            "cook-1", "KITCHEN", "API_STATUS", "Accepted"),
        new OrderStatusChange(null, OrderStatus.CREATED, new Date(1000),
            "owner-1", "USER", "API_CREATE", "ORDER_CREATED")));

    OrderDetailResponse result = mapper.toDetail(order);

    assertEquals(OrderStatus.CREATED, result.getStatusHistory().get(0).getToStatus());
    assertEquals(OrderStatus.ACCEPTED, result.getStatusHistory().get(1).getToStatus());
  }

  @Test
  public void shouldUseSeparateAdminEndpointContract() {
    TacoOrder order = order();
    when(service.findAll("user-a", OrderStatus.PLACED, 0, 20))
        .thenReturn(Mono.just(new OrderHistoryPage(List.of(order), 0, 20, 1, 1)));

    assertEquals(1, new AdminOrderHistoryController(service, mapper)
        .findAll("user-a", OrderStatus.PLACED, 0, 20).block().getTotalElements());
  }

  private TacoOrder order() {
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setPlacedAt(new Date());
    order.setStatus(OrderStatus.PLACED);
    order.setSubtotal(new BigDecimal("100.00"));
    order.setDiscount(BigDecimal.ZERO.setScale(2));
    order.setTotal(new BigDecimal("100.00"));
    order.setCurrency("MXN");
    order.setItems(List.of());
    return order;
  }
}
