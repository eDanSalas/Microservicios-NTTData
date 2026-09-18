package tacos.web.api.dto;

import java.util.ArrayList;
import java.util.List;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;

import lombok.Data;

@Data
@JsonIgnoreProperties({
    "id",
    "placedAt",
    "status",
    "subtotal",
    "discount",
    "total",
    "currency",
    "userId",
    "user"
})
public class OrderCreateRequest {

  @NotBlank
    @Size(max = 100)
    private String deliveryName;

    @NotBlank
    @Size(max = 120)
    private String deliveryStreet;

    @NotBlank
    @Size(max = 80)
    private String deliveryCity;

    @NotBlank
    @Size(max = 80)
    private String deliveryState;

    @NotBlank
    @Pattern(
        regexp = "\\d{5}",
        message = "must contain exactly 5 digits")
    private String deliveryZip;

    @NotBlank
    private String paymentMethodId;

    @Size(max = 40)
    private String couponCode;

    @Valid
    @NotNull
    @Size(
        min = 1,
        max = 20,
        message = "must contain between 1 and 20 items")
    @JsonSetter(nulls = Nulls.FAIL)
    private List<OrderItemRequest> items = new ArrayList<>();

  @JsonAnySetter
  public void rejectUnknownProperty(String propertyName, Object value) {
    throw new IllegalArgumentException("Unknown order property: " + propertyName);
  }
}
