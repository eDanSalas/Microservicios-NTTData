package tacos.service;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(KitchenQueueProperties.class)
public class KitchenQueueConfiguration {
}
