package tacos.service;

import org.springframework.boot.context.properties.ConfigurationProperties;

import lombok.Data;

@Data
@ConfigurationProperties(prefix = "tacocloud.kitchen")
public class KitchenQueueProperties {
  private String stationId = "station-main";
  private int baseMinutes = 5;
  private int queuedOrderMinutes = 2;
  private int itemMinutes = 3;
  private int ingredientMinutes = 1;
}
