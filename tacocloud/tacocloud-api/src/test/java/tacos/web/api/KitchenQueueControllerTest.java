package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.OrderItem;
import tacos.OrderStatus;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.service.KitchenOrderView;
import tacos.service.KitchenQueueService;
import tacos.web.api.dto.KitchenOrderResponse;
import tacos.web.api.dto.OrderStatusUpdateRequest;

public class KitchenQueueControllerTest {
  @Test
  public void shouldReturnOnlyKitchenSafeFields() {
    KitchenQueueService service = mock(KitchenQueueService.class);
    User cook = mock(User.class);
    Taco taco = new Taco();
    taco.setName("Safe taco");
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setDeliveryName("Secret name");
    order.setDeliveryStreet("Secret address");
    order.setPaymentMethodId("secret-token");
    order.setItems(List.of(new OrderItem(taco, 2, BigDecimal.ONE, BigDecimal.TWO)));
    when(service.queue(cook)).thenReturn(Flux.just(new KitchenOrderView(order, 15)));

    JsonNode json = new ObjectMapper().valueToTree(
        new KitchenQueueController(service).queue(cook).blockFirst());

    assertTrue(json.has("estimatedPrepMinutes"));
    assertTrue(json.has("items"));
    assertFalse(json.has("deliveryName"));
    assertFalse(json.has("deliveryStreet"));
    assertFalse(json.has("paymentMethodId"));
    assertFalse(json.has("userId"));
  }

  @Test
  public void shouldExposeKitchenStatusUpdate() {
    KitchenQueueService service = mock(KitchenQueueService.class);
    User cook = mock(User.class);
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setStatus(OrderStatus.PREPARING);
    OrderStatusUpdateRequest request = new OrderStatusUpdateRequest();
    request.setStatus(OrderStatus.PREPARING);
    when(service.updateStatus("order-1", OrderStatus.PREPARING, null, cook))
        .thenReturn(Mono.just(new KitchenOrderView(order, 0)));

    KitchenOrderResponse response = new KitchenQueueController(service)
        .updateStatus("order-1", request, cook).block();

    assertTrue(response != null && response.getStatus() == OrderStatus.PREPARING);
  }
}
