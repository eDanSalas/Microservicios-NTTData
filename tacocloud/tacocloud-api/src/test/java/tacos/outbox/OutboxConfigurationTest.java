package tacos.outbox;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.data.mongodb.ReactiveMongoDatabaseFactory;

class OutboxConfigurationTest {

  @Test
  void shouldProvideOutboxClockWhenAnotherClockExists() {
    new ApplicationContextRunner()
        .withBean("otherClock", Clock.class, () -> Clock.fixed(Instant.EPOCH, ZoneOffset.UTC))
        .withBean(ReactiveMongoDatabaseFactory.class, () -> mock(ReactiveMongoDatabaseFactory.class))
        .withUserConfiguration(OutboxConfiguration.class)
        .run(context -> {
          assertThat(context).hasNotFailed().hasBean("outboxClock");
          assertThat(context.getBeansOfType(Clock.class)).containsKeys("otherClock", "outboxClock");
        });
  }
}
