package tacos.outbox;

import java.time.Duration;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.validation.annotation.Validated;

@ConfigurationProperties("tacocloud.outbox")
@Validated
public class OutboxProperties {

  @Min(1)
  private int batchSize = 20;
  @Min(1)
  private int maxAttempts = 5;
  @Min(1)
  private long publishDelayMs = 1000;
  @NotNull
  private Duration initialBackoff = Duration.ofSeconds(1);
  @NotNull
  private Duration maxBackoff = Duration.ofMinutes(5);
  @NotNull
  private Duration claimTimeout = Duration.ofMinutes(1);

  public int getBatchSize() {
    return batchSize;
  }

  public void setBatchSize(int batchSize) {
    this.batchSize = batchSize;
  }

  public int getMaxAttempts() {
    return maxAttempts;
  }

  public void setMaxAttempts(int maxAttempts) {
    this.maxAttempts = maxAttempts;
  }

  public long getPublishDelayMs() {
    return publishDelayMs;
  }

  public void setPublishDelayMs(long publishDelayMs) {
    this.publishDelayMs = publishDelayMs;
  }

  public Duration getInitialBackoff() {
    return initialBackoff;
  }

  public void setInitialBackoff(Duration initialBackoff) {
    this.initialBackoff = initialBackoff;
  }

  public Duration getMaxBackoff() {
    return maxBackoff;
  }

  public void setMaxBackoff(Duration maxBackoff) {
    this.maxBackoff = maxBackoff;
  }

  public Duration getClaimTimeout() {
    return claimTimeout;
  }

  public void setClaimTimeout(Duration claimTimeout) {
    this.claimTimeout = claimTimeout;
  }
}
