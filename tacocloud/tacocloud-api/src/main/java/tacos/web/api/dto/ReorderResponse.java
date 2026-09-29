package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.List;

import lombok.Value;

@Value
public class ReorderResponse {
  String status;
  String sourceOrderId;
  OrderResponse order;
  BigDecimal previousTotal;
  BigDecimal currentTotal;
  String currency;
  boolean confirmationRequired;
  List<ReorderDifferenceResponse> differences;
}
