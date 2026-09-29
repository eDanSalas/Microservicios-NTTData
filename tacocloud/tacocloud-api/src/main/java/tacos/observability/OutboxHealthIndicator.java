package tacos.observability;

import java.util.List;

import org.springframework.boot.actuate.health.Health;
import org.springframework.boot.actuate.health.ReactiveHealthIndicator;
import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;
import tacos.data.OutboxEventRepository;
import tacos.data.OutboxStatus;

@Component("outboxHealthIndicator")
public class OutboxHealthIndicator implements ReactiveHealthIndicator {

  private final OutboxEventRepository outbox;

  public OutboxHealthIndicator(OutboxEventRepository outbox) {
    this.outbox = outbox;
  }

  @Override
  public Mono<Health> health() {
    return outbox.countByStatusIn(List.of(OutboxStatus.NEW, OutboxStatus.PUBLISHING,
        OutboxStatus.FAILED)).map(count -> Health.up().withDetail("component", "outbox")
            .withDetail("pending", count).build())
        .onErrorResume(error -> Mono.just(Health.down()
            .withDetail("component", "outbox")
            .withDetail("error", error.getClass().getSimpleName()).build()));
  }
}
