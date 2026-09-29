package tacos.observability;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.springframework.boot.actuate.health.Status;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.data.OutboxEventRepository;

public class OutboxHealthIndicatorTest {

  @Test
  public void shouldExplainDegradedOutboxWithoutSensitiveDetails() {
    OutboxEventRepository outbox = mock(OutboxEventRepository.class);
    when(outbox.countByStatusIn(anyCollection())).thenReturn(Mono.error(
        new IllegalStateException("mongodb://user:secret@host")));

    StepVerifier.create(new OutboxHealthIndicator(outbox).health()).assertNext(health -> {
      assertEquals(Status.DOWN, health.getStatus());
      assertEquals("outbox", health.getDetails().get("component"));
      assertEquals("IllegalStateException", health.getDetails().get("error"));
    }).verifyComplete();
  }
}
