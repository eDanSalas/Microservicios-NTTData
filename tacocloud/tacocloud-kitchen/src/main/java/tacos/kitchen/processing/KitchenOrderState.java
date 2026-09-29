package tacos.kitchen.processing;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import tacos.messaging.OrderEvent;

@Document("kitchen_order_states")
public class KitchenOrderState {

  @Id
  private String orderId;
  private String status;
  private String stationId;
  private String cookId;
  private String lastEventId;
  private int appliedEvents;
  private Instant updatedAt;

  public KitchenOrderState() {
  }

  public KitchenOrderState(String orderId) {
    this.orderId = orderId;
  }

  public void apply(OrderEvent event, Instant now) {
    status = event.getPayload().getStatus();
    stationId = event.getPayload().getStationId();
    cookId = event.getPayload().getCookId();
    lastEventId = event.getEventId();
    appliedEvents++;
    updatedAt = now;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getStatus() {
    return status;
  }

  public String getLastEventId() {
    return lastEventId;
  }

  public int getAppliedEvents() {
    return appliedEvents;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }
}
