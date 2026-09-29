package tacos.web.api.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class TacoRatingRequest {
  @NotNull
  @Min(1)
  @Max(5)
  private Integer score;
}
