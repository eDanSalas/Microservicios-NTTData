package tacos.payment;

import java.time.Clock;
import java.time.YearMonth;
import java.util.Date;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.PaymentMethod;
import tacos.User;
import tacos.data.PaymentMethodRepository;
import tacos.web.api.dto.PaymentMethodResponse;
import tacos.web.api.dto.PaymentTokenizationRequest;

@Service
public class PaymentMethodService {

  private final PaymentGateway paymentGateway;
  private final PaymentMethodRepository repository;
  private final Clock clock;

  @Autowired
  public PaymentMethodService(
      PaymentGateway paymentGateway,
      PaymentMethodRepository repository) {

    this(
        paymentGateway,
        repository,
        Clock.systemUTC());
  }

  PaymentMethodService(
      PaymentGateway paymentGateway,
      PaymentMethodRepository repository,
      Clock clock) {

    this.paymentGateway = paymentGateway;
    this.repository = repository;
    this.clock = clock;
  }

  public Mono<PaymentMethodResponse> tokenize(
      PaymentTokenizationRequest request,
      User authenticatedUser) {

    String userId =
        authenticatedUser != null
            ? authenticatedUser.getId()
            : null;

    if (userId == null) {
      return Mono.error(
          new ResponseStatusException(
              HttpStatus.UNAUTHORIZED,
              "Authentication is required"));
    }

    YearMonth expiration =
        YearMonth.of(
            request.getExpirationYear(),
            request.getExpirationMonth());

    if (expiration.isBefore(
        YearMonth.now(clock))) {

      return Mono.error(
          new ResponseStatusException(
              HttpStatus.BAD_REQUEST,
              "Payment card is expired"));
    }

    PaymentCardData paymentCard =
        new PaymentCardData(
            request.getPan(),
            request.getExpirationMonth(),
            request.getExpirationYear(),
            request.getSecurityCode());

    return paymentGateway
        .tokenize(paymentCard)
        .map(tokenized ->
            new PaymentMethod(
                null,
                userId,
                tokenized.getPaymentToken(),
                tokenized.getBrand(),
                tokenized.getLast4(),
                tokenized.getExpirationMonth(),
                tokenized.getExpirationYear(),
                Date.from(clock.instant())))
        .flatMap(repository::save)
        .map(PaymentMethodResponse::from);
  }
}