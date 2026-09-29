package tacos.web.api.dto;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import tacos.OrderStatus;

@Data
public class OrderStatusUpdateRequest {
  @NotNull
  private OrderStatus status;
  @Size(max = 200)
  private String reason;

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown status property: " + propertyName);
  }
}
