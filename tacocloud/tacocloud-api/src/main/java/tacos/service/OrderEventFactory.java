package tacos.service;

import java.util.List;
import java.util.UUID;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.OrderItem;
import tacos.TacoOrder;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

@Component
public class OrderEventFactory {
  public OrderEvent created(TacoOrder order) {
    return created(order, UUID.randomUUID().toString());
  }

  public OrderEvent created(TacoOrder order, String correlationId) {
    return event(OrderEventType.ORDER_CREATED, order, correlationId);
  }

  public OrderEvent event(OrderEventType type, TacoOrder order) {
    return event(type, order, UUID.randomUUID().toString());
  }

  public OrderEvent event(OrderEventType type, TacoOrder order, String correlationId) {
    List<OrderEventPayload.Item> items = order.getItems() == null ? List.of()
        : order.getItems().stream().map(this::item).toList();
    OrderEventPayload payload = new OrderEventPayload(order.getId(), order.getStatus().name(),
        order.getStationId(), order.getCookId(), items);
    return OrderEvent.create(type, correlationId, payload);
  }

  private OrderEventPayload.Item item(OrderItem item) {
    List<String> ingredients = item.getTaco().getIngredients() == null ? List.of()
        : item.getTaco().getIngredients().stream().map(Ingredient::getName).toList();
    return new OrderEventPayload.Item(item.getTaco().getName(), item.getQuantity(), ingredients);
  }
}
