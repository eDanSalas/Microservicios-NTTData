package tacos.pricing;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class PricingConfiguration {

  @Bean
  public Clock pricingClock() {
    return Clock.systemUTC();
  }
}
