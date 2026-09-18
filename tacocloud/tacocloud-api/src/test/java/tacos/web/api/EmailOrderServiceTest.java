package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.PaymentMethod;
import tacos.User;
import tacos.data.IngredientRepository;
import tacos.data.PaymentMethodRepository;
import tacos.data.UserRepository;
import tacos.web.api.EmailOrder.EmailTaco;

public class EmailOrderServiceTest {

  private UserRepository userRepo;
  private IngredientRepository ingredientRepo;
  private PaymentMethodRepository paymentMethodRepo;
  private EmailOrderService service;

  @BeforeEach
  public void setUp() {
    userRepo =
        Mockito.mock(UserRepository.class);

    ingredientRepo =
        Mockito.mock(IngredientRepository.class);

    paymentMethodRepo =
        Mockito.mock(PaymentMethodRepository.class);

    service = new EmailOrderService(
        userRepo,
        ingredientRepo,
        paymentMethodRepo);
  }

  @Test
  public void shouldConvertOrderWithSeveralTacos() {
    User user = testUser();

    PaymentMethod paymentMethod =
        testPaymentMethod(user);

    Ingredient flour =
        new Ingredient(
            "FLTO",
            "Flour Tortilla",
            Ingredient.Type.WRAP);

    Ingredient beef =
        new Ingredient(
            "GRBF",
            "Ground Beef",
            Ingredient.Type.PROTEIN);

    Ingredient tomato =
        new Ingredient(
            "TMTO",
            "Tomatoes",
            Ingredient.Type.VEGGIES);

    when(userRepo.findByEmail(
            "daniel@example.com"))
        .thenReturn(Mono.just(user));

    when(paymentMethodRepo.findAllByUserIdOrderByIdDesc(
            "user-1"))
        .thenReturn(Flux.just(paymentMethod));

    when(ingredientRepo.findById("FLTO"))
        .thenReturn(Mono.just(flour));

    when(ingredientRepo.findById("GRBF"))
        .thenReturn(Mono.just(beef));

    when(ingredientRepo.findById("TMTO"))
        .thenReturn(Mono.just(tomato));

    EmailOrder emailOrder =
        emailOrder(
            taco(
                "Beef taco",
                "FLTO",
                "GRBF"),
            taco(
                "Tomato taco",
                "FLTO",
                "TMTO"));

    StepVerifier.create(
            service.convertEmailOrderToDomainOrder(
                Mono.just(emailOrder)))
        .assertNext(order -> {
            assertSame(
              user,
              order.getUser());

            assertEquals(
              "Daniel",
              order.getDeliveryName());

            assertEquals("pm-1", order.getPaymentMethodId());
            assertEquals("VISA", order.getPaymentBrand());
            assertEquals("1111", order.getPaymentLast4());

            assertEquals(
              2,
              order.getTacos().size());

            assertEquals(
              "Beef taco",
              order.getTacos()
                  .get(0)
                  .getName());

            assertEquals(
              List.of(flour, beef),
              order.getTacos()
                  .get(0)
                  .getIngredients());

            assertEquals(
              "Tomato taco",
              order.getTacos()
                  .get(1)
                  .getName());

            assertEquals(
              List.of(flour, tomato),
              order.getTacos()
                  .get(1)
                  .getIngredients());
        })
        .verifyComplete();

    verify(userRepo, times(1))
        .findByEmail("daniel@example.com");

    verify(paymentMethodRepo, times(1))
        .findAllByUserIdOrderByIdDesc("user-1");

    verify(ingredientRepo, times(2))
        .findById("FLTO");

    verify(ingredientRepo, times(1))
        .findById("GRBF");

    verify(ingredientRepo, times(1))
        .findById("TMTO");
  }

  @Test
  public void shouldRejectUnknownIngredient() {
    User user = testUser();

    PaymentMethod paymentMethod =
        testPaymentMethod(user);

    Ingredient flour =
        new Ingredient(
            "FLTO",
            "Flour Tortilla",
            Ingredient.Type.WRAP);

    when(userRepo.findByEmail(
            "daniel@example.com"))
        .thenReturn(Mono.just(user));

    when(paymentMethodRepo.findAllByUserIdOrderByIdDesc(
            "user-1"))
        .thenReturn(Flux.just(paymentMethod));

    when(ingredientRepo.findById("FLTO"))
        .thenReturn(Mono.just(flour));

    when(ingredientRepo.findById("UNKNOWN"))
        .thenReturn(Mono.empty());

    EmailOrder emailOrder =
        emailOrder(
            taco(
                "Broken taco",
                "FLTO",
                "UNKNOWN"));

    StepVerifier.create(
            service.convertEmailOrderToDomainOrder(
                Mono.just(emailOrder)))
        .expectErrorSatisfies(error ->
            assertConversionError(
                error,
                "Unknown ingredient id: UNKNOWN"))
        .verify();

    verify(ingredientRepo)
        .findById("FLTO");

    verify(ingredientRepo)
        .findById("UNKNOWN");
  }

  @Test
  public void shouldRejectMissingUser() {
    when(userRepo.findByEmail(
            "daniel@example.com"))
        .thenReturn(Mono.empty());

    EmailOrder emailOrder =
        emailOrder(
            taco(
                "Beef taco",
                "FLTO"));

    StepVerifier.create(
            service.convertEmailOrderToDomainOrder(
                Mono.just(emailOrder)))
        .expectErrorSatisfies(error ->
            assertConversionError(
                error,
                "User not found for email order"))
        .verify();

    verify(userRepo)
        .findByEmail("daniel@example.com");

    verifyNoInteractions(
        paymentMethodRepo,
        ingredientRepo);
  }

  @Test
  public void shouldRejectMissingPaymentMethod() {
    User user = testUser();

    when(userRepo.findByEmail(
            "daniel@example.com"))
        .thenReturn(Mono.just(user));

    when(paymentMethodRepo.findAllByUserIdOrderByIdDesc(
            "user-1"))
        .thenReturn(Flux.empty());

    EmailOrder emailOrder =
        emailOrder(
            taco(
                "Beef taco",
                "FLTO"));

    StepVerifier.create(
            service.convertEmailOrderToDomainOrder(
                Mono.just(emailOrder)))
        .expectErrorSatisfies(error ->
            assertConversionError(
                error,
                "Payment method not found for user"))
        .verify();

    verify(userRepo)
        .findByEmail("daniel@example.com");

    verify(paymentMethodRepo)
        .findAllByUserIdOrderByIdDesc("user-1");

    verifyNoInteractions(ingredientRepo);
  }

  private void assertConversionError(
      Throwable error,
      String expectedReason) {

    assertTrue(
        error instanceof ResponseStatusException);

    ResponseStatusException exception =
        (ResponseStatusException) error;

    assertEquals(
        HttpStatus.UNPROCESSABLE_ENTITY,
        exception.getStatus());

    assertEquals(
        expectedReason,
        exception.getReason());
  }

  private User testUser() {
    User user = new User(
        "daniel",
        "{noop}password",
        "Daniel",
        "Main Street 123",
        "Guadalajara",
        "Jalisco",
        "44100",
        "3312345678",
        "daniel@example.com");

    user.setId("user-1");

    return user;
  }

  private PaymentMethod testPaymentMethod(User user) {

        return new PaymentMethod(
        "pm-1",
        user.getId(),
        "token-test",
        "VISA",
        "1111",
        12,
        2099,
        new Date());
  }

  private EmailOrder emailOrder(
      EmailTaco... tacos) {

    EmailOrder order = new EmailOrder();
    order.setEmail("daniel@example.com");
    order.setTacos(List.of(tacos));

    return order;
  }

  private EmailTaco taco(
      String name,
      String... ingredientIds) {

    EmailTaco taco = new EmailTaco();
    taco.setName(name);
    taco.setIngredients(
        List.of(ingredientIds));

    return taco;
  }
}