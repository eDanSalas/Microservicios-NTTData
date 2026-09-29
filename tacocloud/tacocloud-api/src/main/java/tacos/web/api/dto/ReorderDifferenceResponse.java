package tacos.web.api.dto;

import lombok.Value;

@Value
public class ReorderDifferenceResponse {
  String type;
  String tacoName;
  String previousValue;
  String currentValue;
}
