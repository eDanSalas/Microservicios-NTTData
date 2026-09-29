package tacos.messaging;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.core.KafkaTemplate;

@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "kafka")
public class KafkaMessagingConfiguration {

  @Bean
  public OrderMessagingService kafkaOrderMessagingService(
      KafkaTemplate<String, OrderEvent> kafkaTemplate,
      @Value("${tacocloud.messaging.kafka.topic}") String topic) {
    return new KafkaOrderMessagingService(kafkaTemplate, topic);
  }

}
