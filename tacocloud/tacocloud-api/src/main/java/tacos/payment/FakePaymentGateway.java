package tacos.payment;

import java.util.UUID;
import java.util.regex.Pattern;

import org.springframework.stereotype.Component;

import reactor.core.publisher.Mono;

@Component
public class FakePaymentGateway implements PaymentGateway {

  private static final Pattern PAN = Pattern.compile("\\d{13,19}");

  @Override
  public Mono<TokenizedPayment> tokenize(PaymentCardData paymentCard) {

    return Mono.defer(() -> { 
    
        validate(paymentCard);

        String pan = paymentCard.getPan();

        String last4 = pan.substring(pan.length() - 4);

        String token =
            "tok_fake_"
                + UUID.randomUUID()
                    .toString()
                    .replace("-", "");

        return Mono.just(
            new TokenizedPayment(
                token,
                detectBrand(pan),
                last4,
                paymentCard.getExpirationMonth(),
                paymentCard.getExpirationYear()));
    });
  }

  private void validate(PaymentCardData paymentCard) {

    if (paymentCard == null
        || paymentCard.getPan() == null
        || !PAN.matcher(
            paymentCard.getPan()).matches()
        || paymentCard.getSecurityCode() == null
        || paymentCard.getSecurityCode()
            .isBlank()) {

      throw new IllegalArgumentException(
          "Invalid synthetic payment data");
    }
  }

  private String detectBrand(String pan) {
    if (pan.startsWith("4")) {
      return "VISA";
    }

    if (pan.matches("^5[1-5].*")) {
      return "MASTERCARD";
    }

    if (pan.matches("^3[47].*")) {
      return "AMEX";
    }

    if (pan.startsWith("6")) {
      return "DISCOVER";
    }

    return "OTHER";
  }
}