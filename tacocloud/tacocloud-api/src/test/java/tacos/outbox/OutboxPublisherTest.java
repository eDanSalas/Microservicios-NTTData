package tacos.outbox;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.data.OutboxEvent;
import tacos.data.OutboxEventRepository;
import tacos.messaging.OrderEvent;
import tacos.messaging.OrderEventPayload;
import tacos.messaging.OrderEventType;
import tacos.messaging.OrderMessagingService;

public class OutboxPublisherTest {

  private static final Instant NOW = Instant.parse("2026-09-21T12:00:00Z");
  private OutboxEventRepository outboxRepo;
  private OrderMessagingService orderMessages;
  private OutboxProperties properties;
  private OutboxPublisher publisher;

  @BeforeEach
  public void setUp() {
    outboxRepo = Mockito.mock(OutboxEventRepository.class);
    orderMessages = Mockito.mock(OrderMessagingService.class);
    properties = new OutboxProperties();
    properties.setInitialBackoff(Duration.ofSeconds(2));
    properties.setMaxBackoff(Duration.ofSeconds(10));
    publisher = new OutboxPublisher(outboxRepo, orderMessages, properties,
        Clock.fixed(NOW, ZoneOffset.UTC));
  }

  @Test
  public void shouldKeepFailedEventRetryableAndPublishAfterRecovery() {
    OutboxEvent event = claimedEvent(2);
    when(outboxRepo.claimBatch(any(), eq(20), eq(5), any()))
        .thenReturn(Flux.just(event), Flux.just(event));
    when(outboxRepo.markFailed(eq(event), any(), any(), eq("broker unavailable")))
        .thenReturn(Mono.just(event));
    when(outboxRepo.markPublished(eq(event), any())).thenReturn(Mono.just(event));
    doThrow(new IllegalStateException("broker unavailable")).doNothing()
        .when(orderMessages).sendOrder(event.getEvent());

    StepVerifier.create(publisher.publishBatch()).expectNext(1L).verifyComplete();
    StepVerifier.create(publisher.publishBatch()).expectNext(1L).verifyComplete();

    ArgumentCaptor<Instant> retryAt = ArgumentCaptor.forClass(Instant.class);
    verify(outboxRepo).markFailed(eq(event), eq(NOW), retryAt.capture(),
        eq("broker unavailable"));
    assertEquals(NOW.plusSeconds(4), retryAt.getValue());
    verify(outboxRepo).markPublished(event, NOW);
    verify(orderMessages, times(2)).sendOrder(event.getEvent());
  }

  @Test
  public void shouldProcessPendingEventWithAReplacementPublisher() {
    OutboxEvent event = claimedEvent(1);
    OutboxPublisher replacement = new OutboxPublisher(outboxRepo, orderMessages, properties,
        Clock.fixed(NOW, ZoneOffset.UTC));
    when(outboxRepo.claimBatch(any(), eq(20), eq(5), any())).thenReturn(Flux.just(event));
    when(outboxRepo.markPublished(event, NOW)).thenReturn(Mono.just(event));
    doNothing().when(orderMessages).sendOrder(event.getEvent());

    StepVerifier.create(replacement.publishBatch()).expectNext(1L).verifyComplete();

    verify(orderMessages).sendOrder(event.getEvent());
    verify(outboxRepo).markPublished(event, NOW);
  }

  private OutboxEvent claimedEvent(int attempts) {
    OrderEvent event = OrderEvent.create(OrderEventType.ORDER_CREATED,
        UUID.randomUUID().toString(), new OrderEventPayload("order-1", "CREATED", null, null,
            List.of()));
    OutboxEvent outbox = OutboxEvent.pending(event, NOW);
    outbox.claimed(UUID.randomUUID().toString(), attempts, NOW);
    return outbox;
  }
}
