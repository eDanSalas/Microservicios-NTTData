package tacos.service;

import java.time.Clock;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OrderWorkflowConfiguration {
  @Bean
  public Clock orderWorkflowClock() {
    return Clock.systemUTC();
  }
}
