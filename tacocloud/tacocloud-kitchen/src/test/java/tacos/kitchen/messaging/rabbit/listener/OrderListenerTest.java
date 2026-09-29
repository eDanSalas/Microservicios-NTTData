package tacos.kitchen.messaging.rabbit.listener;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tacos.kitchen.KitchenUI;
import tacos.kitchen.KitchenMetrics;
import tacos.kitchen.processing.KitchenEventProcessor;
import tacos.kitchen.processing.ProcessingResult;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

public class OrderListenerTest {

  @Test
  public void shouldNotRepeatEffectAfterCrashBeforeAcknowledgement() {
    KitchenUI ui = Mockito.mock(KitchenUI.class);
    KitchenEventProcessor processor = Mockito.mock(KitchenEventProcessor.class);
    OrderEvent event = OrderEvent.create(OrderEventType.ORDER_CREATED,
        UUID.randomUUID().toString(), new OrderEventPayload("order-1", "CREATED", null, null,
            List.of()));
    when(processor.process(event)).thenReturn(ProcessingResult.PROCESSED,
        ProcessingResult.DUPLICATE);
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    OrderListener listener = new OrderListener(ui, processor, new KitchenMetrics(registry));

    listener.receiveOrder(event);
    listener.receiveOrder(event);

    verify(processor, times(2)).process(event);
    verify(ui, times(1)).displayOrder(event);
    org.junit.jupiter.api.Assertions.assertEquals(2,
        registry.get("tacocloud.kitchen.latency").timer().count());
  }
}
