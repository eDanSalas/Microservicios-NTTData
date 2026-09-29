package tacos.kitchen.processing;

import java.time.Clock;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.mongodb.MongoDatabaseFactory;
import org.springframework.data.mongodb.MongoTransactionManager;

@Configuration
public class KitchenProcessingConfiguration {

  @Bean
  @ConditionalOnMissingBean
  public MongoTransactionManager mongoTransactionManager(MongoDatabaseFactory factory) {
    return new MongoTransactionManager(factory);
  }

  @Bean
  @ConditionalOnMissingBean
  public Clock kitchenClock() {
    return Clock.systemUTC();
  }
}
