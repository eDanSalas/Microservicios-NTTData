package tacos.observability;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import io.micrometer.core.instrument.MeterRegistry;
import reactor.core.publisher.Mono;
import tacos.OrderStatus;
import tacos.data.OrderRepository;
import tacos.data.OutboxEventRepository;
import tacos.data.OutboxStatus;

@Component
public class BacklogMetrics {

  private final OutboxEventRepository outbox;
  private final OrderRepository orders;
  private final AtomicLong outboxPending = new AtomicLong(-1);
  private final AtomicLong kitchenQueued = new AtomicLong(-1);

  public BacklogMetrics(MeterRegistry registry, OutboxEventRepository outbox,
      OrderRepository orders) {
    this.outbox = outbox;
    this.orders = orders;
    registry.gauge("tacocloud.outbox.pending", outboxPending);
    registry.gauge("tacocloud.kitchen.queue", kitchenQueued);
  }

  @Scheduled(fixedDelayString = "${tacocloud.metrics.backlog-delay-ms:5000}")
  public void update() {
    refresh().subscribe();
  }

  Mono<Void> refresh() {
    return Mono.zip(outbox.countByStatusIn(List.of(OutboxStatus.NEW, OutboxStatus.PUBLISHING,
        OutboxStatus.FAILED)), orders.countByStatus(OrderStatus.CREATED)).doOnNext(counts -> {
          outboxPending.set(counts.getT1());
          kitchenQueued.set(counts.getT2());
        }).doOnError(error -> {
          outboxPending.set(-1);
          kitchenQueued.set(-1);
        }).onErrorResume(error -> Mono.empty()).then();
  }
}
