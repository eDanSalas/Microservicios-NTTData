package tacos.web.api.dto;

import javax.validation.constraints.NotBlank;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;

@Data
public class ReorderRequest {
  @NotBlank
  private String paymentMethodId;
  private boolean confirmPriceChange;

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown reorder property: " + propertyName);
  }
}
