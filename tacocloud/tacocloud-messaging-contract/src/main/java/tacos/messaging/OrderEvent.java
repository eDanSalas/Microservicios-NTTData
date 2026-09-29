package tacos.messaging;

import java.time.Instant;
import java.time.format.DateTimeParseException;
import java.util.Objects;
import java.util.UUID;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public final class OrderEvent {
  public static final int CURRENT_VERSION = 1;
  private final String eventId;
  private final OrderEventType eventType;
  private final int version;
  private final String occurredAt;
  private final String correlationId;
  private final OrderEventPayload payload;

  @JsonCreator
  public OrderEvent(@JsonProperty("eventId") String eventId,
      @JsonProperty("eventType") OrderEventType eventType,
      @JsonProperty("version") int version, @JsonProperty("occurredAt") String occurredAt,
      @JsonProperty("correlationId") String correlationId,
      @JsonProperty("payload") OrderEventPayload payload) {
    this.eventId = uuid(eventId, "eventId");
    this.eventType = Objects.requireNonNull(eventType, "eventType is required");
    if (version < 1) throw new IllegalArgumentException("version must be positive");
    this.version = version;
    this.occurredAt = instant(occurredAt);
    this.correlationId = uuid(correlationId, "correlationId");
    this.payload = Objects.requireNonNull(payload, "payload is required");
  }

  public static OrderEvent create(OrderEventType type, String correlationId,
      OrderEventPayload payload) {
    return new OrderEvent(UUID.randomUUID().toString(), type, CURRENT_VERSION,
        Instant.now().toString(), correlationId, payload);
  }

  public String getEventId() {
    return eventId;
  }

  public OrderEventType getEventType() {
    return eventType;
  }

  public int getVersion() {
    return version;
  }

  public String getOccurredAt() {
    return occurredAt;
  }

  public String getCorrelationId() {
    return correlationId;
  }

  public OrderEventPayload getPayload() {
    return payload;
  }

  private static String uuid(String value, String field) {
    try {
      return UUID.fromString(value).toString();
    } catch (IllegalArgumentException | NullPointerException error) {
      throw new IllegalArgumentException(field + " must be a UUID", error);
    }
  }

  private static String instant(String value) {
    try {
      return Instant.parse(value).toString();
    } catch (DateTimeParseException | NullPointerException error) {
      throw new IllegalArgumentException("occurredAt must be an instant", error);
    }
  }
}
