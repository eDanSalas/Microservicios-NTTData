package tacos.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Set;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.inventory.InsufficientStockException;

public class BusinessMetricsTest {

  @Test
  public void shouldCountAndTimeBusinessResultsWithAllowedTags() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    BusinessMetrics metrics = new BusinessMetrics(registry);

    StepVerifier.create(metrics.placement(Mono.just("order"), "api", true))
        .expectNext("order").verifyComplete();
    StepVerifier.create(metrics.placement(Mono.error(new InsufficientStockException("FLTO")),
        "api", false)).expectError(InsufficientStockException.class).verify();
    metrics.cancelled("workflow");

    assertEquals(1, registry.get("tacocloud.orders").tags("status", "created").counter().count());
    assertEquals(1, registry.get("tacocloud.orders").tags("status", "failed").counter().count());
    assertEquals(1, registry.get("tacocloud.orders").tags("status", "cancelled").counter().count());
    assertEquals(1, registry.get("tacocloud.coupons").counter().count());
    assertEquals(1, registry.get("tacocloud.inventory").counter().count());
    assertEquals(2, registry.get("tacocloud.order.placement").timers().stream()
        .mapToLong(timer -> timer.count()).sum());
    Set<String> allowed = Set.of("status", "source", "result", "transport");
    assertTrue(registry.getMeters().stream().flatMap(meter -> meter.getId().getTags().stream())
        .allMatch(tag -> allowed.contains(tag.getKey())));
  }
}
