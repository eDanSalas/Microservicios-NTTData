package tacos.kitchen.processing;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

@Document("kitchen_processed_events")
public class ProcessedEvent {

  @Id
  private String eventId;
  private String orderId;
  private String result;
  private Instant processedAt;

  public ProcessedEvent() {
  }

  public ProcessedEvent(String eventId, String orderId, String result, Instant processedAt) {
    this.eventId = eventId;
    this.orderId = orderId;
    this.result = result;
    this.processedAt = processedAt;
  }

  public String getEventId() {
    return eventId;
  }

  public String getOrderId() {
    return orderId;
  }

  public String getResult() {
    return result;
  }

  public Instant getProcessedAt() {
    return processedAt;
  }
}
