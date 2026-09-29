package tacos.kitchen.processing;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventType;

@Service
public class KitchenEventProcessor {

  private final ProcessedEventRepository processedEvents;
  private final KitchenEventTransaction transaction;

  public KitchenEventProcessor(ProcessedEventRepository processedEvents,
      KitchenEventTransaction transaction) {
    this.processedEvents = processedEvents;
    this.transaction = transaction;
  }

  public ProcessingResult process(OrderEvent event) {
    validate(event);
    if (processedEvents.existsById(event.getEventId())) return ProcessingResult.DUPLICATE;
    try {
      transaction.apply(event);
      return ProcessingResult.PROCESSED;
    } catch (DuplicateKeyException error) {
      return ProcessingResult.DUPLICATE;
    }
  }

  private void validate(OrderEvent event) {
    if (event.getVersion() != OrderEvent.CURRENT_VERSION)
      throw new PermanentEventException("UNSUPPORTED_VERSION");
    if (event.getEventType() != OrderEventType.ORDER_CREATED
        && event.getEventType() != OrderEventType.STATUS_CHANGED)
      throw new PermanentEventException("UNSUPPORTED_EVENT_TYPE");
  }
}
