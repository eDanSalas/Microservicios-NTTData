package tacos.service;

import java.math.BigDecimal;
import java.util.Map;

import lombok.Value;
import tacos.Taco;

@Value
public class TacoRatingStats {
  Taco taco;
  BigDecimal average;
  long count;
  Map<Integer, Long> distribution;
}
