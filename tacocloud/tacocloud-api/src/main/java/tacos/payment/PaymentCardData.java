package tacos.payment;

import lombok.ToString;
import lombok.Value;

@Value
public class PaymentCardData {

  @ToString.Exclude
  String pan;

  Integer expirationMonth;
  Integer expirationYear;

  @ToString.Exclude
  String securityCode;
}