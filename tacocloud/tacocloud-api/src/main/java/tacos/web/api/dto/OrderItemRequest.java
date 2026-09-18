package tacos.web.api.dto;

import javax.validation.Valid;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;

@Data
public class OrderItemRequest {

  @Valid
  @NotNull
  private OrderTacoRequest taco;
  @Min(1)
  private int quantity;

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown order item property: " + propertyName);
  }
}
