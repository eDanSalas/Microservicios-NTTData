package tacos.kitchen;

import java.util.List;

import org.slf4j.MDC;
import org.springframework.stereotype.Component;

import lombok.extern.slf4j.Slf4j;
import tacos.messaging.OrderEvent;

@Component
@Slf4j
public class KitchenUI {

  public void displayOrder(OrderEvent event) {
    displayStatus(event);
  }

  public void displayQueue(List<OrderEvent> events) {
    events.forEach(event -> withCorrelation(event,
        () -> log.info("QUEUED ORDER: id={}, status={}, items={}",
            event.getPayload().getOrderId(), event.getPayload().getStatus(),
            event.getPayload().getItems().size())));
  }

  public void displayClaim(OrderEvent event, int estimatedPrepMinutes) {
    withCorrelation(event,
        () -> log.info("CLAIMED ORDER: id={}, status={}, station={}, cook={}, etaMinutes={}",
            event.getPayload().getOrderId(), event.getPayload().getStatus(),
            event.getPayload().getStationId(), event.getPayload().getCookId(),
            estimatedPrepMinutes));
  }

  public void displayStatus(OrderEvent event) {
    withCorrelation(event,
        () -> log.info("ORDER EVENT: eventId={}, type={}, orderId={}, status={}, station={}",
            event.getEventId(), event.getEventType(), event.getPayload().getOrderId(),
            event.getPayload().getStatus(), event.getPayload().getStationId()));
  }

  private void withCorrelation(OrderEvent event, Runnable action) {
    try (MDC.MDCCloseable ignored = MDC.putCloseable("correlationId", event.getCorrelationId())) {
      action.run();
    }
  }
}
