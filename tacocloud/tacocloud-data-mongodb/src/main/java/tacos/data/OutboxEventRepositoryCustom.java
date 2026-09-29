package tacos.data;

import java.time.Duration;
import java.time.Instant;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

public interface OutboxEventRepositoryCustom {
  Flux<OutboxEvent> claimBatch(Instant now, int batchSize, int maxAttempts,
      Duration claimTimeout);
  Mono<OutboxEvent> markPublished(OutboxEvent event, Instant now);
  Mono<OutboxEvent> markFailed(OutboxEvent event, Instant now, Instant nextAttemptAt,
      String error);
}
