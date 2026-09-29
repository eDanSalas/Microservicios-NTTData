package tacos.service;

import lombok.Value;

@Value
public class ReorderDifference {
  String type;
  String tacoName;
  String previousValue;
  String currentValue;
}
