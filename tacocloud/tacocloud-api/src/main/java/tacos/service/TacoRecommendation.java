package tacos.service;

import java.time.LocalDate;

import lombok.Value;
import tacos.Taco;

@Value
public class TacoRecommendation {
  Taco taco;
  LocalDate date;
  String reason;
}
