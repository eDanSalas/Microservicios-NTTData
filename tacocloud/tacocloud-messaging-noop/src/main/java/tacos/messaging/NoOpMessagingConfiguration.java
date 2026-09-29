package tacos.messaging;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@ConditionalOnProperty(name = "tacocloud.messaging.transport", havingValue = "noop",
    matchIfMissing = true)
public class NoOpMessagingConfiguration {

  @Bean
  public OrderMessagingService noOpOrderMessagingService() {
    return new NoOpOrderMessagingService();
  }

}
