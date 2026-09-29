package tacos.web.api.dto;

import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;

@Data
public class OrderCancellationRequest {
  @Size(max = 200)
  private String reason;

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown cancellation property: " + propertyName);
  }
}
