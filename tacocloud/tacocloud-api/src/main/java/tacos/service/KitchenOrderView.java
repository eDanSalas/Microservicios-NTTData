package tacos.service;

import lombok.Value;
import tacos.TacoOrder;

@Value
public class KitchenOrderView {
  TacoOrder order;
  int estimatedPrepMinutes;
}
