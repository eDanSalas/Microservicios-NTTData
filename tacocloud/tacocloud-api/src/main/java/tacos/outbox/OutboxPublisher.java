package tacos.outbox;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;
import tacos.data.OutboxEvent;
import tacos.data.OutboxEventRepository;
import tacos.messaging.OrderMessagingService;

@Service
public class OutboxPublisher {

  private final OutboxEventRepository outboxRepo;
  private final OrderMessagingService orderMessages;
  private final OutboxProperties properties;
  private final Clock clock;

  public OutboxPublisher(OutboxEventRepository outboxRepo, OrderMessagingService orderMessages,
      OutboxProperties properties, @Qualifier("outboxClock") Clock clock) {
    this.outboxRepo = outboxRepo;
    this.orderMessages = orderMessages;
    this.properties = properties;
    this.clock = clock;
  }

  @Scheduled(fixedDelayString = "${tacocloud.outbox.publish-delay-ms:1000}")
  public void publishPending() {
    publishBatch().subscribe();
  }

  public Mono<Long> publishBatch() {
    Instant now = Instant.now(clock);
    return outboxRepo.claimBatch(now, properties.getBatchSize(), properties.getMaxAttempts(),
        properties.getClaimTimeout()).concatMap(event -> publish(event).thenReturn(event)).count();
  }

  private Mono<Void> publish(OutboxEvent event) {
    return Mono.fromRunnable(() -> orderMessages.sendOrder(event.getEvent()))
        .then(Mono.defer(() -> outboxRepo.markPublished(event, Instant.now(clock))))
        .onErrorResume(error -> markFailed(event, error)).then();
  }

  private Mono<OutboxEvent> markFailed(OutboxEvent event, Throwable error) {
    Instant now = Instant.now(clock);
    return outboxRepo.markFailed(event, now, now.plus(backoff(event.getAttempts())),
        error.getMessage() == null ? error.getClass().getSimpleName() : error.getMessage());
  }

  private Duration backoff(int attempts) {
    Duration delay = properties.getInitialBackoff();
    for (int attempt = 1; attempt < attempts && delay.compareTo(properties.getMaxBackoff()) < 0;
        attempt++) delay = delay.multipliedBy(2);
    return delay.compareTo(properties.getMaxBackoff()) > 0 ? properties.getMaxBackoff() : delay;
  }
}
