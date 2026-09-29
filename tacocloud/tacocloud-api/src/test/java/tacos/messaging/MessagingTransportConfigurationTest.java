package tacos.messaging;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.Test;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.context.ApplicationContext;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.kafka.core.KafkaTemplate;
import org.springframework.util.StreamUtils;

class MessagingTransportConfigurationTest {

  private final JmsTemplate jms = mock(JmsTemplate.class);
  private final RabbitTemplate rabbit = mock(RabbitTemplate.class);
  private final KafkaTemplate<String, OrderEvent> kafka = mock(KafkaTemplate.class);
  private final ApplicationContextRunner runner = new ApplicationContextRunner()
      .withUserConfiguration(MessagingTransportConfiguration.class)
      .withBean(JmsTemplate.class, () -> jms)
      .withBean(RabbitTemplate.class, () -> rabbit)
      .withBean("kafkaTemplate", KafkaTemplate.class, () -> kafka)
      .withPropertyValues("tacocloud.messaging.jms.destination=orders.jms",
          "tacocloud.messaging.rabbit.exchange=orders.exchange",
          "tacocloud.messaging.rabbit.routing-key=orders.rabbit",
          "tacocloud.messaging.kafka.topic=orders.kafka");

  @Test
  void activatesNoopByDefault() {
    runner.run(context -> assertSingle(context, NoOpOrderMessagingService.class));
  }

  @Test
  void activatesJmsWithExternalDestination() {
    runner.withPropertyValues("tacocloud.messaging.transport=jms").run(context -> {
      OrderMessagingService service = assertSingle(context, JmsOrderMessagingService.class);
      OrderEvent event = event();
      service.sendOrder(event);
      verify(jms).convertAndSend(eq("orders.jms"), eq(event),
          any(org.springframework.jms.core.MessagePostProcessor.class));
    });
  }

  @Test
  void activatesRabbitWithExternalDestination() {
    runner.withPropertyValues("tacocloud.messaging.transport=rabbit").run(context -> {
      OrderMessagingService service = assertSingle(context, RabbitOrderMessagingService.class);
      OrderEvent event = event();
      service.sendOrder(event);
      verify(rabbit).convertAndSend(eq("orders.exchange"), eq("orders.rabbit"), eq(event),
          any(org.springframework.amqp.core.MessagePostProcessor.class));
    });
  }

  @Test
  void activatesKafkaWithExternalDestination() {
    runner.withPropertyValues("tacocloud.messaging.transport=kafka").run(context -> {
      OrderMessagingService service = assertSingle(context, KafkaOrderMessagingService.class);
      OrderEvent event = event();
      service.sendOrder(event);
      verify(kafka).send("orders.kafka", event);
    });
  }

  @Test
  void rejectsUnknownTransport() {
    runner.withPropertyValues("tacocloud.messaging.transport=pigeon").run(context -> {
      assertThat(context).hasFailed();
      assertThat(context.getStartupFailure()).hasMessageContaining("tacocloud.messaging.transport");
    });
  }

  @Test
  void rejectsNoopInProduction() {
    runner.withPropertyValues("spring.profiles.active=production",
        "tacocloud.messaging.transport=noop").run(context -> {
          assertThat(context).hasFailed();
          assertThat(context.getStartupFailure()).hasMessageContaining(
              "Messaging transport noop is not allowed in production");
        });
  }

  @Test
  void brokerCredentialsHaveNoCommittedValues() throws Exception {
    StringBuilder configuration = new StringBuilder();
    java.util.Enumeration<java.net.URL> resources = getClass().getClassLoader()
        .getResources("application.yml");
    while (resources.hasMoreElements()) {
      try (java.io.InputStream input = resources.nextElement().openStream()) {
        configuration.append(StreamUtils.copyToString(input,
            java.nio.charset.StandardCharsets.UTF_8));
      }
    }
    assertThat(configuration).doesNotContain("letm31n", "l3tm31n")
        .contains("${ARTEMIS_PASSWORD:}", "${RABBITMQ_PASSWORD:}");
  }

  private OrderMessagingService assertSingle(ApplicationContext context, Class<?> type) {
    assertThat(context.getBeansOfType(OrderMessagingService.class)).hasSize(1);
    OrderMessagingService service = context.getBean(OrderMessagingService.class);
    assertThat(service).isInstanceOf(type);
    return service;
  }

  private OrderEvent event() {
    return OrderEvent.create(OrderEventType.ORDER_CREATED, UUID.randomUUID().toString(),
        new OrderEventPayload("order-1", "PLACED", null, null, List.of()));
  }

}
