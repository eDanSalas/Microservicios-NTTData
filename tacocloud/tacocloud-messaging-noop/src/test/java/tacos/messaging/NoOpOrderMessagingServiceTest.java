package tacos.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;

public class NoOpOrderMessagingServiceTest {

  @Test
  public void logContainsOnlyOrderIdentifier() {
    Logger logger =
        (Logger) LoggerFactory.getLogger(
            NoOpOrderMessagingService.class);

    ListAppender<ILoggingEvent> appender =
        new ListAppender<>();

    appender.start();
    logger.addAppender(appender);

    try {
      OrderEvent event = OrderEvent.create(OrderEventType.ORDER_CREATED,
          UUID.randomUUID().toString(),
          new OrderEventPayload("order-1", "CREATED", null, null, List.of()));

      new NoOpOrderMessagingService()
          .sendOrder(event);

      List<ILoggingEvent> events =
          appender.list;

      String message =
          events.get(0)
              .getFormattedMessage();

      assertTrue(
          message.contains("order-1"));

      assertFalse(
          message.contains("payment"));

      assertFalse(
          message.contains("password"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}
