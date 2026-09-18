package tacos.web.api.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import tacos.PaymentMethod;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class PaymentMethodResponse {

  private String paymentMethodId;
  private String brand;
  private String last4;

  public static PaymentMethodResponse from(
      PaymentMethod paymentMethod) {

    return new PaymentMethodResponse(
        paymentMethod.getId(),
        paymentMethod.getBrand(),
        paymentMethod.getLast4());
  }
}