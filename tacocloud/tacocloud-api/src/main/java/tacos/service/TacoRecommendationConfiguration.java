package tacos.service;

import java.time.Clock;
import java.time.ZoneId;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class TacoRecommendationConfiguration {

  @Bean
  public Clock tacoRecommendationClock() {
    return Clock.systemUTC();
  }

  @Bean
  public ZoneId tacoRecommendationZone(
      @Value("${tacocloud.taco-of-the-day.zone:America/Mexico_City}") String zone) {
    return ZoneId.of(zone);
  }
}
