package tacos.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.OrderStatus;
import tacos.data.OrderRepository;
import tacos.data.OutboxEventRepository;

public class BacklogMetricsTest {

  @Test
  public void shouldRefreshGaugesWithoutBlocking() {
    SimpleMeterRegistry registry = new SimpleMeterRegistry();
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    OrderRepository orders = mock(OrderRepository.class);
    when(outbox.countByStatusIn(anyCollection())).thenReturn(Mono.just(7L));
    when(orders.countByStatus(OrderStatus.CREATED)).thenReturn(Mono.just(3L));
    BacklogMetrics metrics = new BacklogMetrics(registry, outbox, orders);

    StepVerifier.create(metrics.refresh()).verifyComplete();

    assertEquals(7, registry.get("tacocloud.outbox.pending").gauge().value());
    assertEquals(3, registry.get("tacocloud.kitchen.queue").gauge().value());
  }
}
