package tacos.web.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;

@Data
@JsonIgnoreProperties({
    "id",
    "createdAt"
})
public class OrderTacoRequest {

  @NotBlank
    @Size(
        min = 5,
        max = 50,
        message = "must contain between 5 and 50 characters")
    private String name;

    @NotNull
    @Size(
        min = 1,
        max = 10,
        message = "must contain between 1 and 10 ingredient ids")
    @JsonSetter(nulls = Nulls.FAIL)
    private List<@NotBlank String> ingredientIds = new ArrayList<>();

  @JsonAnySetter
  public void rejectUnknownProperty(
      String propertyName,
      Object value) {

    throw new IllegalArgumentException(
        "Unknown taco property: "
            + propertyName);
  }
}
