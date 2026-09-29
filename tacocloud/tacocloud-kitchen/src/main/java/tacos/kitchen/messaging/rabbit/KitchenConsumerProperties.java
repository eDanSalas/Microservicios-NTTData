package tacos.kitchen.messaging.rabbit;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@Validated
@ConfigurationProperties("tacocloud.consumer")
public class KitchenConsumerProperties {

  @Min(1)
  private int maxAttempts = 3;
  @Min(1)
  private long initialBackoffMs = 100;
  @Min(1)
  private long maxBackoffMs = 2000;
  @DecimalMin("1.0")
  private double multiplier = 2;
  private String deadLetterExchange = "tacocloud.order.dlx";
  private String deadLetterQueue = "tacocloud.order.dlq";
  private String deadLetterRoutingKey = "tacocloud.order.dead";

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getInitialBackoffMs() {
    return initialBackoffMs;
  }

  public void setInitialBackoffMs(long initialBackoffMs) {
    this.initialBackoffMs = initialBackoffMs;
  }

  public long getMaxBackoffMs() {
    return maxBackoffMs;
  }

  public void setMaxBackoffMs(long maxBackoffMs) {
    this.maxBackoffMs = maxBackoffMs;
  }

  public double getMultiplier() {
    return multiplier;
  }

  public void setMultiplier(double multiplier) {
    this.multiplier = multiplier;
  }

  public String getDeadLetterExchange() {
    return deadLetterExchange;
  }

  public void setDeadLetterExchange(String deadLetterExchange) {
    this.deadLetterExchange = deadLetterExchange;
  }

  public String getDeadLetterQueue() {
    return deadLetterQueue;
  }

  public void setDeadLetterQueue(String deadLetterQueue) {
    this.deadLetterQueue = deadLetterQueue;
  }

  public String getDeadLetterRoutingKey() {
    return deadLetterRoutingKey;
  }

  public void setDeadLetterRoutingKey(String deadLetterRoutingKey) {
    this.deadLetterRoutingKey = deadLetterRoutingKey;
  }
}
