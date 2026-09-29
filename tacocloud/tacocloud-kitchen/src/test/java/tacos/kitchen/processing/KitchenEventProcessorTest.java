package tacos.kitchen.processing;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

public class KitchenEventProcessorTest {

  private ProcessedEventRepository processedEvents;
  private KitchenEventTransaction transaction;
  private KitchenEventProcessor processor;

  @BeforeEach
  public void setUp() {
    processedEvents = Mockito.mock(ProcessedEventRepository.class);
    transaction = Mockito.mock(KitchenEventTransaction.class);
    processor = new KitchenEventProcessor(processedEvents, transaction);
  }

  @Test
  public void shouldApplyTheSameEventOnlyOnce() {
    OrderEvent event = event(OrderEvent.CURRENT_VERSION, OrderEventType.ORDER_CREATED);
    when(processedEvents.existsById(event.getEventId())).thenReturn(false, true);

    assertEquals(ProcessingResult.PROCESSED, processor.process(event));
    assertEquals(ProcessingResult.DUPLICATE, processor.process(event));

    verify(transaction, times(1)).apply(event);
  }

  @Test
  public void shouldTreatConcurrentUniqueKeyAsDuplicate() {
    OrderEvent event = event(OrderEvent.CURRENT_VERSION, OrderEventType.ORDER_CREATED);
    when(processedEvents.existsById(event.getEventId())).thenReturn(false);
    doThrow(new org.springframework.dao.DuplicateKeyException("duplicate"))
        .when(transaction).apply(event);

    assertEquals(ProcessingResult.DUPLICATE, processor.process(event));
  }

  @Test
  public void shouldRejectUnknownVersionPermanently() {
    OrderEvent event = event(OrderEvent.CURRENT_VERSION + 1, OrderEventType.ORDER_CREATED);

    PermanentEventException error = assertThrows(PermanentEventException.class,
        () -> processor.process(event));

    assertEquals("UNSUPPORTED_VERSION", error.getMessage());
    verify(processedEvents, times(0)).existsById(event.getEventId());
  }

  private OrderEvent event(int version, OrderEventType type) {
    return new OrderEvent(UUID.randomUUID().toString(), type, version, Instant.now().toString(),
        UUID.randomUUID().toString(), new OrderEventPayload("order-1", "CREATED", null, null,
            List.of()));
  }
}
