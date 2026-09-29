package tacos.messaging;

import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.jms.core.JmsTemplate;
import org.springframework.jms.support.converter.MappingJackson2MessageConverter;

@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "jms")
public class JmsMessagingConfiguration {

  @Bean
  public OrderMessagingService jmsOrderMessagingService(JmsTemplate jms,
      @Value("${tacocloud.messaging.jms.destination}") String destination) {
    return new JmsOrderMessagingService(jms, destination);
  }

  @Bean
  public MappingJackson2MessageConverter jmsMessageConverter() {
    MappingJackson2MessageConverter converter = new MappingJackson2MessageConverter();
    converter.setTypeIdPropertyName("_typeId");
    converter.setTypeIdMappings(Map.of("orderEvent", OrderEvent.class));
    return converter;
  }

}
