package tacos.messaging;

import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.amqp.support.converter.Jackson2JsonMessageConverter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "rabbit")
public class RabbitMessagingConfiguration {

  @Bean
  public OrderMessagingService rabbitOrderMessagingService(RabbitTemplate rabbit,
      @Value("${tacocloud.messaging.rabbit.exchange}") String exchange,
      @Value("${tacocloud.messaging.rabbit.routing-key}") String routingKey) {
    return new RabbitOrderMessagingService(rabbit, exchange, routingKey);
  }

  @Bean
  public Jackson2JsonMessageConverter rabbitMessageConverter() {
    return new Jackson2JsonMessageConverter();
  }

}
