package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Value;

@Value
public class TacoRatingResponse {
  TacoResponse taco;
  BigDecimal average;
  long count;
  Map<Integer, Long> distribution;
}
