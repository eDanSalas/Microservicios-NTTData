package tacos.web.api.dto;

import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.Data;

@Data
public class StockAdjustmentRequest {

  @NotNull
  private Integer quantity;
  @NotNull
  @Min(0)
  private Long version;
}
