package tacos.web.api.dto;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.DecimalMin;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import lombok.Data;

@Data
public class OrderQuoteRequest {

  @NotNull
  @DecimalMin("0.00")
  private BigDecimal subtotal;

  @NotBlank
  @Size(max = 40)
  private String couponCode;

  @Valid
  private List<OrderItemRequest> items = new ArrayList<>();
}
