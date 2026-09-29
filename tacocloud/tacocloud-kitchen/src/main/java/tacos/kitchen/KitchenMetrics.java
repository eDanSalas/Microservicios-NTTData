package tacos.kitchen;

import java.time.Duration;
import java.time.Instant;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import tacos.kitchen.processing.ProcessingResult;
import tacos.messaging.OrderEvent;

@Component
public class KitchenMetrics {

  private final MeterRegistry registry;

  public KitchenMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public void received(OrderEvent event, ProcessingResult result, String transport) {
    registry.counter("tacocloud.kitchen.events", "result", result.name().toLowerCase(),
        "transport", transport).increment();
    Duration latency = Duration.between(Instant.parse(event.getOccurredAt()), Instant.now());
    registry.timer("tacocloud.kitchen.latency", "transport", transport)
        .record(latency.isNegative() ? Duration.ZERO : latency);
  }

  public void received(OrderEvent event, String transport) {
    received(event, ProcessingResult.PROCESSED, transport);
  }
}
