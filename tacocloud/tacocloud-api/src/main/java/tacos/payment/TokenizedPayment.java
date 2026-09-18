package tacos.payment;

import lombok.ToString;
import lombok.Value;

@Value
public class TokenizedPayment {

  @ToString.Exclude
  String paymentToken;

  String brand;
  String last4;
  Integer expirationMonth;
  Integer expirationYear;
}