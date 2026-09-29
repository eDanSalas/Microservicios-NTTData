package tacos.service;

import java.math.BigDecimal;
import java.util.List;

import lombok.Value;
import tacos.TacoOrder;

@Value
public class ReorderResult {
  String status;
  String sourceOrderId;
  TacoOrder order;
  BigDecimal previousTotal;
  BigDecimal currentTotal;
  String currency;
  boolean confirmationRequired;
  List<ReorderDifference> differences;
}
