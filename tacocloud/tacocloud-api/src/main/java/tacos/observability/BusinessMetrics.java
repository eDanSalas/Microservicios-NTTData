package tacos.observability;

import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import reactor.core.publisher.Mono;
import tacos.inventory.InsufficientStockException;

@Component
public class BusinessMetrics {

  private final MeterRegistry registry;

  public BusinessMetrics(MeterRegistry registry) {
    this.registry = registry;
  }

  public <T> Mono<T> placement(Mono<T> operation, String source, boolean couponRequested) {
    return Mono.defer(() -> {
      Timer.Sample sample = Timer.start(registry);
      return operation.doOnNext(value -> {
        registry.counter("tacocloud.orders", "status", "created", "source", source).increment();
        if (couponRequested) registry.counter("tacocloud.coupons", "result", "applied").increment();
      }).doOnError(error -> {
        registry.counter("tacocloud.orders", "status", "failed", "source", source).increment();
        if (hasCause(error, InsufficientStockException.class))
          registry.counter("tacocloud.inventory", "result", "rejected").increment();
      }).doFinally(signal -> sample.stop(registry.timer("tacocloud.order.placement",
          "result", signal.name().equals("onComplete") ? "success" : "failure")));
    });
  }

  public void cancelled(String source) {
    registry.counter("tacocloud.orders", "status", "cancelled", "source", source).increment();
  }

  private boolean hasCause(Throwable error, Class<? extends Throwable> type) {
    for (Throwable cause = error; cause != null; cause = cause.getCause())
      if (type.isInstance(cause)) return true;
    return false;
  }
}
