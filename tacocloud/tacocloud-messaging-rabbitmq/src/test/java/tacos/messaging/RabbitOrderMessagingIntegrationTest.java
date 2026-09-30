package tacos.messaging;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.UUID;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.rabbit.connection.CachingConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitAdmin;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.testcontainers.containers.RabbitMQContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

@Testcontainers(disabledWithoutDocker = true)
class RabbitOrderMessagingIntegrationTest {

  private static final String EXCHANGE = "orders.integration";
  private static final String ROUTING_KEY = "orders.created";
  private static final String QUEUE = "orders.integration.queue";

  @Container
  static final RabbitMQContainer RABBIT = new RabbitMQContainer(
      DockerImageName.parse("rabbitmq:3.13-management-alpine"));

  private CachingConnectionFactory connectionFactory;
  private RabbitTemplate template;

  @BeforeEach
  void setUp() {
    connectionFactory = new CachingConnectionFactory(RABBIT.getHost(), RABBIT.getAmqpPort());
    connectionFactory.setUsername(RABBIT.getAdminUsername());
    connectionFactory.setPassword(RABBIT.getAdminPassword());
    template = new RabbitTemplate(connectionFactory);
    template.setMessageConverter(new Jackson2JsonMessageConverter());
    RabbitAdmin admin = new RabbitAdmin(connectionFactory);
    DirectExchange exchange = new DirectExchange(EXCHANGE);
    Queue queue = new Queue(QUEUE, false, false, true);
    admin.declareExchange(exchange);
    admin.declareQueue(queue);
    admin.declareBinding(BindingBuilder.bind(queue).to(exchange).with(ROUTING_KEY));
  }

  @AfterEach
  void tearDown() {
    connectionFactory.destroy();
  }

  @Test
  void shouldPublishHeadersAndRedeliverUnacknowledgedOrder() throws Exception {
    String correlationId = UUID.randomUUID().toString();
    OrderEvent event = OrderEvent.create(OrderEventType.ORDER_CREATED, correlationId,
        new OrderEventPayload("order-36", "CREATED", null, null, List.of()));
    new RabbitOrderMessagingService(template, EXCHANGE, ROUTING_KEY).sendOrder(event);

    com.rabbitmq.client.ConnectionFactory rawFactory = new com.rabbitmq.client.ConnectionFactory();
    rawFactory.setUri(RABBIT.getAmqpUrl());
    try (com.rabbitmq.client.Connection connection = rawFactory.newConnection();
        com.rabbitmq.client.Channel channel = connection.createChannel()) {
      com.rabbitmq.client.GetResponse delivery = channel.basicGet(QUEUE, false);
      assertThat(delivery).isNotNull();
      // AMQP table strings are decoded as LongString by the RabbitMQ client.
      assertThat(delivery.getProps().getHeaders().get("X_ORDER_SOURCE").toString())
          .isEqualTo("WEB");
      assertThat(delivery.getProps().getHeaders().get("X_CORRELATION_ID").toString())
          .isEqualTo(correlationId);
    }

    Message redelivery = template.receive(QUEUE, 5000);
    assertThat(redelivery).isNotNull();
    assertThat(redelivery.getMessageProperties().isRedelivered()).isTrue();
  }
}
