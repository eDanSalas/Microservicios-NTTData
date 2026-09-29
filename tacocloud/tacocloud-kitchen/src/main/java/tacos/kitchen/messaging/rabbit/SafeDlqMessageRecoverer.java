package tacos.kitchen.messaging.rabbit;

import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageDeliveryMode;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.rabbit.retry.MessageRecoverer;

import io.micrometer.core.instrument.MeterRegistry;
import tacos.kitchen.processing.PermanentEventException;

public class SafeDlqMessageRecoverer implements MessageRecoverer {

  private final RabbitTemplate rabbit;
  private final MeterRegistry metrics;
  private final String exchange;
  private final String routingKey;
  private final String sourceQueue;

  public SafeDlqMessageRecoverer(RabbitTemplate rabbit, MeterRegistry metrics, String exchange,
      String routingKey, String sourceQueue) {
    this.rabbit = rabbit;
    this.metrics = metrics;
    this.exchange = exchange;
    this.routingKey = routingKey;
    this.sourceQueue = sourceQueue;
  }

  @Override
  public void recover(Message message, Throwable cause) {
    MessageProperties source = message.getMessageProperties();
    MessageProperties target = new MessageProperties();
    target.setContentType(source.getContentType());
    target.setContentEncoding(source.getContentEncoding());
    target.setDeliveryMode(MessageDeliveryMode.PERSISTENT);
    copyHeader(source, target, "__TypeId__");
    copyHeader(source, target, "X_CORRELATION_ID");
    target.setHeader("X_FAILURE_CAUSE", failureCause(cause));
    target.setHeader("X_ORIGINAL_QUEUE", sourceQueue);
    rabbit.send(exchange, routingKey, new Message(message.getBody(), target));
    metrics.counter("tacocloud.kitchen.events", "result", "dlq", "transport", "rabbit")
        .increment();
  }

  private void copyHeader(MessageProperties source, MessageProperties target, String name) {
    Object value = source.getHeaders().get(name);
    if (value != null) target.setHeader(name, value);
  }

  private String failureCause(Throwable error) {
    for (Throwable cause = error; cause != null; cause = cause.getCause())
      if (cause instanceof PermanentEventException) return "PERMANENT_EVENT";
    return "RETRIES_EXHAUSTED";
  }
}
