package tacos.web.api.dto;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import lombok.Data;

@Data
@JsonIgnoreProperties({
    "name",
    "type"
})
public class IngredientReferenceRequest {

  @NotBlank
    @Size(max = 50)
    @Pattern(
        regexp = "[A-Za-z0-9_-]+",
        message =
            "must contain only letters, numbers, underscores or hyphens")
    private String id;

  @JsonAnySetter
  public void rejectUnknownProperty(
      String propertyName,
      Object value) {

    throw new IllegalArgumentException(
        "Unknown ingredient reference property: "
            + propertyName);
  }
}