package tacos.kitchen;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;

public class KitchenUITest {

  @Test
  public void shouldLogEventCorrelationIdAndCleanMdc() {
    Logger logger = (Logger) LoggerFactory.getLogger(KitchenUI.class);
    ListAppender<ILoggingEvent> events = new ListAppender<>();
    events.start();
    logger.addAppender(events);
    OrderEvent event = OrderEvent.create(OrderEventType.ORDER_CREATED,
        UUID.randomUUID().toString(), new OrderEventPayload("order-1", "CREATED", null, null,
            List.of()));

    new KitchenUI().displayOrder(event);

    assertEquals(event.getCorrelationId(),
        events.list.get(0).getMDCPropertyMap().get("correlationId"));
    assertNull(MDC.get("correlationId"));
    logger.detachAppender(events);
  }
}
