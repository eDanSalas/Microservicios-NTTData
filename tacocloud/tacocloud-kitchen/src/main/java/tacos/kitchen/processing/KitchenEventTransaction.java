package tacos.kitchen.processing;

import java.time.Clock;
import java.time.Instant;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import tacos.messaging.OrderEvent;

@Service
public class KitchenEventTransaction {

  private final ProcessedEventRepository processedEvents;
  private final KitchenOrderStateRepository orderStates;
  private final Clock clock;

  public KitchenEventTransaction(ProcessedEventRepository processedEvents,
      KitchenOrderStateRepository orderStates, Clock clock) {
    this.processedEvents = processedEvents;
    this.orderStates = orderStates;
    this.clock = clock;
  }

  @Transactional
  public void apply(OrderEvent event) {
    Instant now = Instant.now(clock);
    processedEvents.insert(new ProcessedEvent(event.getEventId(),
        event.getPayload().getOrderId(), ProcessingResult.PROCESSED.name(), now));
    KitchenOrderState state = orderStates.findById(event.getPayload().getOrderId())
        .orElseGet(() -> new KitchenOrderState(event.getPayload().getOrderId()));
    state.apply(event, now);
    orderStates.save(state);
  }
}
