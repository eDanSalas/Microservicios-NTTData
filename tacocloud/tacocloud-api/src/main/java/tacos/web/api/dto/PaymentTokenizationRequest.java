package tacos.web.api.dto;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Pattern;

import com.fasterxml.jackson.annotation.JsonAnySetter;
import com.fasterxml.jackson.annotation.JsonProperty;

import org.hibernate.validator.constraints.CreditCardNumber;

import lombok.Data;
import lombok.ToString;

@Data
public class PaymentTokenizationRequest {

  @NotBlank
  @CreditCardNumber
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ToString.Exclude
  private String pan;

  @NotNull
  @Min(1)
  @Max(12)
  private Integer expirationMonth;

  @NotNull
  @Min(2000)
  @Max(2200)
  private Integer expirationYear;

  @NotBlank
  @Pattern(
      regexp = "\\d{3,4}",
      message = "must contain 3 or 4 digits")
  @JsonProperty(access = JsonProperty.Access.WRITE_ONLY)
  @ToString.Exclude
  private String securityCode;

  @JsonAnySetter
  public void rejectUnknownProperty(
      String propertyName,
      Object ignoredValue) {

    throw new IllegalArgumentException(
        "Unknown payment property: "
            + propertyName);
  }
}