package tacos.data;

import java.util.List;

import lombok.Data;
import tacos.Taco;

@Data
public class TacoRatingAggregate {
  private String tacoId;
  private Taco taco;
  private double average;
  private long count;
  private List<RatingScoreCount> distribution;
}
