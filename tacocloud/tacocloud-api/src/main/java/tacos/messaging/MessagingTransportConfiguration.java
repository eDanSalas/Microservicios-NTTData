package tacos.messaging;

import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.SmartInitializingSingleton;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;

@Configuration
@EnableConfigurationProperties(MessagingTransportProperties.class)
@Import({NoOpMessagingConfiguration.class, JmsMessagingConfiguration.class,
    RabbitMessagingConfiguration.class, KafkaMessagingConfiguration.class})
public class MessagingTransportConfiguration {

  @Bean
  public SmartInitializingSingleton messagingTransportValidator(
      MessagingTransportProperties properties, Environment environment,
      ObjectProvider<OrderMessagingService> services) {
    return () -> {
      long count = services.stream().count();
      if (count != 1) {
        throw new IllegalStateException("Exactly one OrderMessagingService is required, found " + count);
      }
      if (properties.getTransport() == MessagingTransportProperties.Transport.NOOP
          && environment.acceptsProfiles(Profiles.of("prod", "production"))) {
        throw new IllegalStateException("Messaging transport noop is not allowed in production");
      }
    };
  }

}
