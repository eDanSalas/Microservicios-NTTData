package tacos.web.api;

import javax.validation.Valid;

import org.springframework.http.HttpStatus;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import reactor.core.publisher.Mono;
import tacos.User;
import tacos.payment.PaymentMethodService;
import tacos.web.api.dto.PaymentMethodResponse;
import tacos.web.api.dto.PaymentTokenizationRequest;

@RestController
@RequestMapping(
    path = "/api/payment-methods",
    produces = "application/json")
public class PaymentMethodController {

  private final PaymentMethodService service;

  public PaymentMethodController(PaymentMethodService service) {
    this.service = service;
  }

  @PostMapping(path = "/tokenize", consumes = "application/json")
  @ResponseStatus(HttpStatus.CREATED)
  public Mono<PaymentMethodResponse> tokenize(@Valid @RequestBody PaymentTokenizationRequest request, @AuthenticationPrincipal User authenticatedUser) {
    return service.tokenize(request, authenticatedUser);
  }
}