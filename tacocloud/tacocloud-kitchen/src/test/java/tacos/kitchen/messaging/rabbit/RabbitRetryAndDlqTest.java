package tacos.kitchen.messaging.rabbit;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.retry.support.RetryTemplate;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import tacos.kitchen.processing.PermanentEventException;
import tacos.kitchen.processing.TransientKitchenException;

public class RabbitRetryAndDlqTest {

  @Test
  public void shouldRetryTransientFailureConfiguredNumberOfTimes() {
    KitchenConsumerProperties properties = properties();
    RetryTemplate retry = new MessagingConfig().kitchenRetryTemplate(properties);
    AtomicInteger attempts = new AtomicInteger();

    assertThrows(TransientKitchenException.class, () -> retry.execute(context -> {
      attempts.incrementAndGet();
      throw new TransientKitchenException("temporary");
    }));

    assertEquals(3, attempts.get());
  }

  @Test
  public void shouldNotRetryPermanentFailure() {
    RetryTemplate retry = new MessagingConfig().kitchenRetryTemplate(properties());
    AtomicInteger attempts = new AtomicInteger();

    assertThrows(PermanentEventException.class, () -> retry.execute(context -> {
      attempts.incrementAndGet();
      throw new PermanentEventException("UNSUPPORTED_VERSION");
    }));

    assertEquals(1, attempts.get());
  }

  @Test
  public void shouldPublishOnlySafeHeadersToDlq() {
    RabbitTemplate rabbit = Mockito.mock(RabbitTemplate.class);
    SimpleMeterRegistry metrics = new SimpleMeterRegistry();
    SafeDlqMessageRecoverer recoverer = new SafeDlqMessageRecoverer(rabbit, metrics,
        "orders.dlx", "orders.dead", "orders");
    MessageProperties source = new MessageProperties();
    source.setContentType(MessageProperties.CONTENT_TYPE_JSON);
    source.setHeader("__TypeId__", "tacos.messaging.OrderEvent");
    source.setHeader("X_CORRELATION_ID", "correlation-1");
    source.setHeader("authorization", "secret");
    byte[] body = "safe-event".getBytes(java.nio.charset.StandardCharsets.UTF_8);

    recoverer.recover(new Message(body, source),
        new PermanentEventException("private internal detail"));

    ArgumentCaptor<Message> sent = ArgumentCaptor.forClass(Message.class);
    verify(rabbit).send(eq("orders.dlx"), eq("orders.dead"), sent.capture());
    assertArrayEquals(body, sent.getValue().getBody());
    assertEquals("PERMANENT_EVENT",
        sent.getValue().getMessageProperties().getHeader("X_FAILURE_CAUSE"));
    assertEquals("correlation-1",
        sent.getValue().getMessageProperties().getHeader("X_CORRELATION_ID"));
    assertFalse(sent.getValue().getMessageProperties().getHeaders().containsKey("authorization"));
    assertEquals(1, metrics.counter("tacocloud.kitchen.events", "result", "dlq",
        "transport", "rabbit").count());
  }

  private KitchenConsumerProperties properties() {
    KitchenConsumerProperties properties = new KitchenConsumerProperties();
    properties.setMaxAttempts(3);
    properties.setInitialBackoffMs(1);
    properties.setMaxBackoffMs(2);
    return properties;
  }
}
