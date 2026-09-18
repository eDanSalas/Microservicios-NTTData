package tacos;

import java.math.BigDecimal;

import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class OrderItem {

  @NotNull
  private Taco taco;
  @Min(1)
  private int quantity;
  @NotNull
  @DecimalMin("0.00")
  private BigDecimal unitPriceAtPurchase;
  @NotNull
  @DecimalMin("0.00")
  private BigDecimal subtotal;
}
