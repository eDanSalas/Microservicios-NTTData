package tacos.web.api;

import java.util.Date;
import java.util.List;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.PaymentMethod;
import tacos.Taco;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.IngredientRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.web.api.EmailOrder.EmailTaco;

@Service
public class EmailOrderService {

  private final UserRepository userRepo;
  private final IngredientRepository ingredientRepo;
  private final PaymentMethodRepository paymentMethodRepo;

  public EmailOrderService(
      UserRepository userRepo,
      IngredientRepository ingredientRepo,
      PaymentMethodRepository paymentMethodRepo) {

    this.userRepo = userRepo;
    this.ingredientRepo = ingredientRepo;
    this.paymentMethodRepo = paymentMethodRepo;
  }

  public Mono<TacoOrder> convertEmailOrderToDomainOrder(
      Mono<EmailOrder> emailOrder) {

    if (emailOrder == null) {
      return conversionError(
          "Email order publisher is required");
    }

    return emailOrder
        .switchIfEmpty(
            conversionError(
                "Email order is required"))
        .flatMap(this::convertEmailOrder);
  }

  private Mono<TacoOrder> convertEmailOrder(
      EmailOrder emailOrder) {

    if (emailOrder.getEmail() == null
        || emailOrder.getEmail().isBlank()) {

      return conversionError(
          "Email is required");
    }

    return userRepo
        .findByEmail(emailOrder.getEmail())
        .switchIfEmpty(
            conversionError(
                "User not found for email order"))
        .flatMap(user ->
            findPaymentMethod(user)
                .flatMap(paymentMethod ->
                    convertTacos(emailOrder.getTacos())
                        .map(tacos ->
                            createOrder(
                                user,
                                paymentMethod,
                                tacos))));
  }

  private Mono<PaymentMethod> findPaymentMethod(User user) {
    return paymentMethodRepo
      .findAllByUserIdOrderByIdDesc(user.getId()).next()
      .switchIfEmpty(conversionError("Payment method not found for user"));
  }

  private Mono<List<Taco>> convertTacos(
      List<EmailTaco> emailTacos) {

    if (emailTacos == null
        || emailTacos.isEmpty()) {

      return conversionError(
          "At least one taco is required");
    }

    return Flux
        .fromIterable(emailTacos)
        .concatMap(this::convertTaco)
        .collectList();
  }

  private Mono<Taco> convertTaco(
      EmailTaco emailTaco) {

    if (emailTaco == null) {
      return conversionError(
          "Taco must not be null");
    }

    if (emailTaco.getName() == null
        || emailTaco.getName().isBlank()) {

      return conversionError(
          "Taco name is required");
    }

    List<String> ingredientIds =
        emailTaco.getIngredients();

    if (ingredientIds == null
        || ingredientIds.isEmpty()) {

      return conversionError(
          "At least one ingredient is required for taco: "
              + emailTaco.getName());
    }

    return Flux
        .fromIterable(ingredientIds)
        .concatMap(this::findIngredient)
        .collectList()
        .map(ingredients -> {
          Taco taco = new Taco();
          taco.setName(emailTaco.getName());
          taco.setIngredients(ingredients);
          return taco;
        });
  }

  private Mono<Ingredient> findIngredient(
      String ingredientId) {

    if (ingredientId == null
        || ingredientId.isBlank()) {

      return conversionError(
          "Ingredient id must not be blank");
    }

    return ingredientRepo
        .findById(ingredientId)
        .switchIfEmpty(
            conversionError(
                "Unknown ingredient id: "
                    + ingredientId));
  }

  private TacoOrder createOrder(
      User user,
      PaymentMethod paymentMethod,
      List<Taco> tacos) {

    TacoOrder order = new TacoOrder();

    order.setUser(user);

    order.setPaymentMethodId(paymentMethod.getId());
    order.setPaymentBrand(paymentMethod.getBrand());
    order.setPaymentLast4(paymentMethod.getLast4());

    order.setDeliveryName(user.getFullname());
    order.setDeliveryStreet(user.getStreet());
    order.setDeliveryCity(user.getCity());
    order.setDeliveryState(user.getState());
    order.setDeliveryZip(user.getZip());

    order.setPlacedAt(new Date());
    order.setTacos(tacos);

    return order;
  }

  private <T> Mono<T> conversionError(
      String reason) {

    return Mono.error(
        new ResponseStatusException(
            HttpStatus.UNPROCESSABLE_ENTITY,
            reason));
  }
}