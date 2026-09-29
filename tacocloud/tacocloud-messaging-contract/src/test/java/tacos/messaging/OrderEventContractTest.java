package tacos.messaging;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

import java.util.List;
import java.util.Locale;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

public class OrderEventContractTest {
  private final ObjectMapper json = new ObjectMapper();

  @Test
  public void shouldSerializeAndDeserializeVersionOne() throws Exception {
    OrderEvent event = event();
    String serialized = json.writeValueAsString(event);
    OrderEvent restored = json.readValue(serialized, OrderEvent.class);

    assertEquals(event.getEventId(), restored.getEventId());
    assertEquals(OrderEventType.ORDER_CREATED, restored.getEventType());
    assertEquals(1, restored.getVersion());
    assertEquals("order-1", restored.getPayload().getOrderId());
    assertEquals("Crunchy", restored.getPayload().getItems().get(0).getTacoName());
  }

  @Test
  public void shouldKeepVersionOneSnapshot() throws Exception {
    assertEquals("{\"eventId\":\"123e4567-e89b-12d3-a456-426614174000\","
        + "\"eventType\":\"ORDER_CREATED\",\"version\":1,"
        + "\"occurredAt\":\"2026-09-21T06:00:00Z\","
        + "\"correlationId\":\"123e4567-e89b-12d3-a456-426614174001\","
        + "\"payload\":{\"orderId\":\"order-1\",\"status\":\"CREATED\","
        + "\"stationId\":null,\"cookId\":null,\"items\":[{\"tacoName\":\"Crunchy\","
        + "\"quantity\":2,\"ingredients\":[\"Beef\",\"Cheese\"]}]}}",
        json.writeValueAsString(event()));
  }

  @Test
  public void shouldExcludeSensitiveFields() throws Exception {
    String serialized = json.writeValueAsString(event()).toLowerCase(Locale.ROOT);

    assertFalse(serialized.contains("pan"));
    assertFalse(serialized.contains("cvv"));
    assertFalse(serialized.contains("password"));
    assertFalse(serialized.contains("userid"));
    assertFalse(serialized.contains("payment"));
  }

  @Test
  public void shouldIgnoreAdditionalCompatibleFields() throws Exception {
    JsonNode tree = json.readTree(json.writeValueAsString(event()));
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree).put("traceHint", "optional");
    ((com.fasterxml.jackson.databind.node.ObjectNode) tree.get("payload"))
        .put("preparationZone", "hot");

    OrderEvent restored = json.treeToValue(tree, OrderEvent.class);

    assertEquals("order-1", restored.getPayload().getOrderId());
    assertEquals(1, restored.getVersion());
  }

  @Test
  public void shouldGenerateUniqueUuidIdentifiers() {
    OrderEvent first = OrderEvent.create(OrderEventType.ORDER_CREATED,
        UUID.randomUUID().toString(), payload());
    OrderEvent second = OrderEvent.create(OrderEventType.ORDER_CREATED,
        UUID.randomUUID().toString(), payload());

    UUID.fromString(first.getEventId());
    UUID.fromString(first.getCorrelationId());
    assertNotEquals(first.getEventId(), second.getEventId());
    assertEquals(OrderEvent.CURRENT_VERSION, first.getVersion());
  }

  private OrderEvent event() {
    return new OrderEvent("123e4567-e89b-12d3-a456-426614174000",
        OrderEventType.ORDER_CREATED, 1, "2026-09-21T06:00:00Z",
        "123e4567-e89b-12d3-a456-426614174001", payload());
  }

  private OrderEventPayload payload() {
    return new OrderEventPayload("order-1", "CREATED", null, null,
        List.of(new OrderEventPayload.Item("Crunchy", 2, List.of("Beef", "Cheese"))));
  }
}
