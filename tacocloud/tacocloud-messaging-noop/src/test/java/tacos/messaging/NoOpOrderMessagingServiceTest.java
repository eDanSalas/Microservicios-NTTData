package tacos.messaging;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import tacos.TacoOrder;

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
      TacoOrder order = new TacoOrder();

      order.setId("order-1");
      order.setPaymentMethodId("pm-secret");
      order.setPaymentBrand("VISA");
      order.setPaymentLast4("1111");

      new NoOpOrderMessagingService()
          .sendOrder(order);

      List<ILoggingEvent> events =
          appender.list;

      String message =
          events.get(0)
              .getFormattedMessage();

      assertTrue(
          message.contains("order-1"));

      assertFalse(
          message.contains("pm-secret"));

      assertFalse(
          message.contains("1111"));
    } finally {
      logger.detachAppender(appender);
      appender.stop();
    }
  }
}