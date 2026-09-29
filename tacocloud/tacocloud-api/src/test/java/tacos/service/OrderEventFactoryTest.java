package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

import tacos.OrderStatus;
import tacos.TacoOrder;
import tacos.User;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventType;

public class OrderEventFactoryTest {
  @Test
  public void shouldMapDomainOrderToSafeCreatedEvent() throws Exception {
    User user = new User("user", "password-secret", "Owner", "Street", "City", "State",
        "00000", "555", "owner@example.com");
    user.setId("user-1");
    TacoOrder order = new TacoOrder();
    order.setId("order-1");
    order.setStatus(OrderStatus.CREATED);
    order.setUser(user);
    order.setPaymentMethodId("payment-secret");
    order.setDeliveryStreet("private-street");

    String correlationId = UUID.randomUUID().toString();
    OrderEvent event = new OrderEventFactory().created(order, correlationId);
    String json = new ObjectMapper().writeValueAsString(event).toLowerCase(Locale.ROOT);

    assertEquals(OrderEventType.ORDER_CREATED, event.getEventType());
    assertEquals("order-1", event.getPayload().getOrderId());
    assertEquals(1, event.getVersion());
    assertNotNull(UUID.fromString(event.getEventId()));
    assertEquals(correlationId, event.getCorrelationId());
    assertFalse(json.contains("password-secret"));
    assertFalse(json.contains("payment-secret"));
    assertFalse(json.contains("private-street"));
    assertFalse(json.contains("user-1"));
  }
}
