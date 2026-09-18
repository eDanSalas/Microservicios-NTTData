package tacos.web.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;

@Data
public class TacoCreateRequest {

  @NotBlank
  @Size(min = 5, max = 80)
  private String name;

  @NotNull
  @Size(min = 1, max = 10)
  private List<@NotBlank String> ingredientIds = new ArrayList<>();

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown taco property: " + propertyName);
  }
}
