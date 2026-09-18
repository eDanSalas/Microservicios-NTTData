package tacos.web.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;

import lombok.Data;
import tacos.Ingredient.Type;

@Data
public class IngredientRequest {

  @NotBlank
  @Size(
      min = 2,
      max = 50,
      message = "must contain between 2 and 50 characters")
  private String name;

  @NotNull
  private Type type;

  @JsonAnySetter
  public void rejectUnknownProperty(
      String propertyName,
      Object value) {

    throw new IllegalArgumentException(
        "Unknown ingredient property: "
            + propertyName);
  }
}