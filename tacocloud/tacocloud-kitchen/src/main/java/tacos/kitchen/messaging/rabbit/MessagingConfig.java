package tacos.kitchen.messaging.rabbit;

import java.util.Map;

import org.aopalliance.intercept.MethodInterceptor;
import org.springframework.amqp.core.AcknowledgeMode;
import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.rabbit.config.RetryInterceptorBuilder;
import org.springframework.amqp.rabbit.config.SimpleRabbitListenerContainerFactory;
import org.springframework.amqp.rabbit.connection.ConnectionFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.autoconfigure.amqp.SimpleRabbitListenerContainerFactoryConfigurer;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.dao.TransientDataAccessException;
import org.springframework.retry.backoff.ExponentialBackOffPolicy;
import org.springframework.retry.policy.SimpleRetryPolicy;
import org.springframework.retry.support.RetryTemplate;

import com.mongodb.MongoException;

import io.micrometer.core.instrument.MeterRegistry;
import tacos.kitchen.processing.TransientKitchenException;

@Profile({"rabbitmq-template", "rabbitmq-listener"})
@Configuration
@EnableConfigurationProperties(KitchenConsumerProperties.class)
public class MessagingConfig {

  @Bean
  public Jackson2JsonMessageConverter messageConverter() {
    return new Jackson2JsonMessageConverter();
  }

  @Bean
  public Queue ordersQueue(@Value("${tacocloud.messaging.rabbit.queue}") String queue,
      KitchenConsumerProperties properties) {
    return QueueBuilder.durable(queue).deadLetterExchange(properties.getDeadLetterExchange())
        .deadLetterRoutingKey(properties.getDeadLetterRoutingKey()).build();
  }

  @Bean
  public DirectExchange deadLetterExchange(KitchenConsumerProperties properties) {
    return new DirectExchange(properties.getDeadLetterExchange());
  }

  @Bean
  public Queue deadLetterQueue(KitchenConsumerProperties properties) {
    return QueueBuilder.durable(properties.getDeadLetterQueue()).build();
  }

  @Bean
  public Binding deadLetterBinding(@Qualifier("deadLetterQueue") Queue deadLetterQueue,
      @Qualifier("deadLetterExchange") DirectExchange deadLetterExchange,
      KitchenConsumerProperties properties) {
    return BindingBuilder.bind(deadLetterQueue).to(deadLetterExchange)
        .with(properties.getDeadLetterRoutingKey());
  }

  @Bean
  public RetryTemplate kitchenRetryTemplate(KitchenConsumerProperties properties) {
    Map<Class<? extends Throwable>, Boolean> retryable = Map.of(
        TransientKitchenException.class, true, TransientDataAccessException.class, true,
        MongoException.class, true);
    RetryTemplate retry = new RetryTemplate();
    retry.setRetryPolicy(new SimpleRetryPolicy(properties.getMaxAttempts(), retryable, true, false));
    ExponentialBackOffPolicy backoff = new ExponentialBackOffPolicy();
    backoff.setInitialInterval(properties.getInitialBackoffMs());
    backoff.setMultiplier(properties.getMultiplier());
    backoff.setMaxInterval(properties.getMaxBackoffMs());
    retry.setBackOffPolicy(backoff);
    return retry;
  }

  @Bean
  public SafeDlqMessageRecoverer safeDlqMessageRecoverer(RabbitTemplate rabbit,
      MeterRegistry metrics, KitchenConsumerProperties properties,
      @Value("${tacocloud.messaging.rabbit.queue}") String queue) {
    return new SafeDlqMessageRecoverer(rabbit, metrics, properties.getDeadLetterExchange(),
        properties.getDeadLetterRoutingKey(), queue);
  }

  @Bean
  public MethodInterceptor kitchenRetryInterceptor(RetryTemplate kitchenRetryTemplate,
      SafeDlqMessageRecoverer recoverer) {
    return RetryInterceptorBuilder.stateless().retryOperations(kitchenRetryTemplate)
        .recoverer(recoverer).build();
  }

  @Bean
  public SimpleRabbitListenerContainerFactory rabbitListenerContainerFactory(
      SimpleRabbitListenerContainerFactoryConfigurer configurer, ConnectionFactory connection,
      Jackson2JsonMessageConverter converter,
      @Qualifier("kitchenRetryInterceptor") MethodInterceptor kitchenRetryInterceptor) {
    SimpleRabbitListenerContainerFactory factory = new SimpleRabbitListenerContainerFactory();
    configurer.configure(factory, connection);
    factory.setMessageConverter(converter);
    factory.setAdviceChain(kitchenRetryInterceptor);
    factory.setAcknowledgeMode(AcknowledgeMode.AUTO);
    factory.setDefaultRequeueRejected(false);
    return factory;
  }
}
