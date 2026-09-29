package tacos.data;

import java.time.Instant;

import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import tacos.messaging.OrderEvent;

@Document("order_outbox")
public class OutboxEvent {

  @Id
  private String eventId;
  private int version;
  private OrderEvent event;
  private OutboxStatus status;
  private int attempts;
  private Instant createdAt;
  private Instant updatedAt;
  private Instant nextAttemptAt;
  private Instant claimedAt;
  private Instant publishedAt;
  private String claimId;
  private String lastError;

  public OutboxEvent() {
  }

  public static OutboxEvent pending(OrderEvent event, Instant now) {
    OutboxEvent outbox = new OutboxEvent();
    outbox.eventId = event.getEventId();
    outbox.version = event.getVersion();
    outbox.event = event;
    outbox.status = OutboxStatus.NEW;
    outbox.createdAt = now;
    outbox.updatedAt = now;
    outbox.nextAttemptAt = now;
    return outbox;
  }

  public void claimed(String claimId, int attempts, Instant now) {
    this.status = OutboxStatus.PUBLISHING;
    this.claimId = claimId;
    this.attempts = attempts;
    this.claimedAt = now;
    this.updatedAt = now;
  }

  public String getEventId() {
    return eventId;
  }

  public int getVersion() {
    return version;
  }

  public OrderEvent getEvent() {
    return event;
  }

  public OutboxStatus getStatus() {
    return status;
  }

  public int getAttempts() {
    return attempts;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public Instant getNextAttemptAt() {
    return nextAttemptAt;
  }

  public Instant getClaimedAt() {
    return claimedAt;
  }

  public Instant getPublishedAt() {
    return publishedAt;
  }

  public String getClaimId() {
    return claimId;
  }

  public String getLastError() {
    return lastError;
  }

}
