package tacos.web.api.dto;

import lombok.Value;

@Value
public class KitchenOrderLineResponse {
  String tacoName;
  int quantity;
}
