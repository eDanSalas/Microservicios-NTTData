package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.never;

import java.math.BigDecimal;
import java.time.Clock;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.TacoOrder;
import tacos.User;
import tacos.data.IngredientRepository;
import tacos.data.OrderRepository;
import tacos.messaging.OrderMessagingService;
import tacos.pricing.CouponProperties;
import tacos.pricing.CouponRule;
import tacos.pricing.CouponService;
import tacos.pricing.CouponType;
import tacos.web.api.dto.OrderCreateRequest;
import tacos.web.api.dto.OrderItemRequest;
import tacos.web.api.dto.OrderTacoRequest;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.OrderMapper;

import java.util.Date;
import tacos.PaymentMethod;
import tacos.data.PaymentMethodRepository;
import tacos.inventory.InventoryService;
import tacos.InventoryReservation;
import tacos.physics.TacoDesignValidationException;
import tacos.physics.AvailableIngredientsRule;
import tacos.physics.BaseCountRule;
import tacos.physics.ExtremeSpiceRequiresBeverageRule;
import tacos.physics.IngredientCountRule;
import tacos.physics.SauceLimitRule;
import tacos.physics.TacoDesignValidator;
import tacos.physics.UniqueIngredientsRule;

public class OrderCreationServiceTest {

  private IngredientRepository ingredientRepo;
  private OrderRepository orderRepo;
  private OrderMessagingService orderMessages;
  private OrderCreationService service;
  private PaymentMethodRepository paymentMethodRepo;
  private InventoryService inventoryService;

  @BeforeEach
  public void setUp() {
    ingredientRepo = Mockito.mock(IngredientRepository.class);

    orderRepo = Mockito.mock(OrderRepository.class);

    orderMessages = Mockito.mock(OrderMessagingService.class);

    paymentMethodRepo = Mockito.mock(PaymentMethodRepository.class);
    inventoryService = Mockito.mock(InventoryService.class);

    when(inventoryService.reserve(Mockito.any(TacoOrder.class), Mockito.anyString()))
        .thenReturn(Mono.just(new InventoryReservation()));
    when(inventoryService.release(Mockito.anyString())).thenReturn(Mono.empty());

    PaymentMethod paymentMethod = new PaymentMethod("pm-1", "owner-1", "token-test",
        "VISA", "1111", 12, 2099, new Date());

    when(paymentMethodRepo.findByIdAndUserId(
        "pm-1", "owner-1"))
        .thenReturn(Mono.just(paymentMethod));
    Ingredient filling = new Ingredient("FILL", "Filling", Type.PROTEIN,
        BigDecimal.ZERO, true, 10, 2, 1L);
    when(ingredientRepo.findById("FILL")).thenReturn(Mono.just(filling));

    OrderMapper orderMapper =
        new OrderMapper(
            new IngredientMapper(),
            new TacoClassificationService());

    CouponRule coupon = new CouponRule();
    coupon.setType(CouponType.PERCENTAGE);
    coupon.setValue(new BigDecimal("10"));
    CouponProperties coupons = new CouponProperties();
    coupons.setCodes(Map.of("SAVE10", coupon));
    TacoDesignValidator validator = new TacoDesignValidator(List.of(new BaseCountRule(),
        new IngredientCountRule(2, 12), new UniqueIngredientsRule(), new AvailableIngredientsRule(),
        new ExtremeSpiceRequiresBeverageRule(true), new SauceLimitRule(3)));
    service = new OrderCreationService(orderRepo, orderMessages, orderMapper,
        paymentMethodRepo, new OrderPricingService(20, "MXN"),
        new CouponService(coupons, Clock.systemUTC()), inventoryService,
        new TacoDesignService(ingredientRepo, validator));
  }

  @Test
  public void shouldResolveSaveAndPublishOrder() {
    User authenticatedUser = testUser();

    Ingredient ingredient =
        new Ingredient(
            "FLTO",
            "Flour Tortilla",
            Type.WRAP,
            new BigDecimal("0.75"),
            true,
            10,
            2,
            1L);

    OrderCreateRequest request =
        testRequest("FLTO");
    request.setCouponCode("save10");

    when(ingredientRepo.findById("FLTO"))
        .thenReturn(Mono.just(ingredient));

    when(orderRepo.save(
            Mockito.any(TacoOrder.class)))
        .thenAnswer(invocation -> {
          TacoOrder saved =
              invocation.getArgument(
                  0,
                  TacoOrder.class);

          saved.setId("order-1");

          return Mono.just(saved);
        });

    StepVerifier.create(
            service.create(
                request,
                authenticatedUser))
        .assertNext(order -> {
          assertEquals(
              "order-1",
              order.getId());

          assertSame(
              authenticatedUser,
              order.getUser());

          assertEquals(new BigDecimal("1.50"), order.getSubtotal());
          assertEquals(new BigDecimal("0.15"), order.getDiscount());
          assertEquals(new BigDecimal("1.35"), order.getTotal());
          assertEquals("SAVE10", order.getCouponCode());
          assertEquals(2, order.getItems().get(0).getQuantity());
          assertEquals("FLTO", order.getItems().get(0)
                  .getTaco()
                  .getIngredients()
                  .get(0)
                  .getId());
        })
        .verifyComplete();

    verify(ingredientRepo)
        .findById("FLTO");

    verify(orderRepo)
        .save(
            Mockito.any(TacoOrder.class));

    verify(orderMessages)
        .sendOrder(
            Mockito.any(TacoOrder.class));
  }

  @Test
  public void shouldRejectUnknownIngredient() {
    User authenticatedUser = testUser();

    OrderCreateRequest request =
        testRequest("UNKNOWN");

    when(ingredientRepo.findById("UNKNOWN"))
        .thenReturn(Mono.empty());

    StepVerifier.create(
            service.create(
                request,
                authenticatedUser))
        .expectErrorSatisfies(error -> {
          ResponseStatusException exception =
              (ResponseStatusException) error;

          assertEquals(
              HttpStatus.UNPROCESSABLE_ENTITY,
              exception.getStatus());

          assertEquals(
              "Unknown ingredient id: UNKNOWN",
              exception.getReason());
        })
        .verify();

    verifyNoInteractions(
        orderRepo,
        orderMessages);
  }

  @Test
  public void shouldReleaseReservationWhenOrderSaveFails() {
    Ingredient ingredient = new Ingredient("FLTO", "Flour Tortilla", Type.WRAP,
        new BigDecimal("0.75"), true, 10, 2, 1L);
    when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(ingredient));
    when(orderRepo.save(Mockito.any(TacoOrder.class))).thenReturn(Mono.error(new RuntimeException("save failed")));

    StepVerifier.create(service.create(testRequest("FLTO"), testUser()))
        .expectErrorMessage("save failed").verify();

    verify(inventoryService).reserve(Mockito.any(TacoOrder.class), Mockito.anyString());
    verify(inventoryService).release(Mockito.anyString());
    verifyNoInteractions(orderMessages);
  }

  @Test
  public void shouldValidateBeforeReservingInventory() {
    Ingredient wrap = new Ingredient("FLTO", "Flour Tortilla", Type.WRAP,
        new BigDecimal("0.75"), true, 10, 2, 1L);
    Ingredient bowl = new Ingredient("BOWL", "Bowl", Type.BOWL,
        new BigDecimal("0.25"), true, 10, 2, 1L);
    when(ingredientRepo.findById("FLTO")).thenReturn(Mono.just(wrap));
    when(ingredientRepo.findById("BOWL")).thenReturn(Mono.just(bowl));
    OrderCreateRequest request = testRequest("FLTO");
    request.getItems().get(0).getTaco().setIngredientIds(List.of("FLTO", "BOWL"));

    StepVerifier.create(service.create(request, testUser()))
        .expectError(TacoDesignValidationException.class).verify();

    verify(inventoryService, never()).reserve(Mockito.any(), Mockito.anyString());
    verify(orderRepo, never()).save(Mockito.any());
    verifyNoInteractions(orderMessages);
  }

  private OrderCreateRequest testRequest(
      String ingredientId) {

    OrderTacoRequest taco =
        new OrderTacoRequest();

    taco.setName("Test taco");
    taco.setIngredientIds(List.of(ingredientId, "FILL"));

    OrderItemRequest item = new OrderItemRequest();
    item.setTaco(taco);
    item.setQuantity(2);

    OrderCreateRequest request =
        new OrderCreateRequest();

    request.setDeliveryName("Daniel");
    request.setDeliveryStreet("Main Street");
    request.setDeliveryCity("Guadalajara");
    request.setDeliveryState("Jalisco");
    request.setDeliveryZip("44100");
    request.setPaymentMethodId("pm-1");
    request.setItems(List.of(item));

    return request;
  }

  private User testUser() {
    User user = new User(
        "daniel",
        "{noop}password",
        "Daniel",
        "Main Street",
        "Guadalajara",
        "Jalisco",
        "44100",
        "3312345678",
        "daniel@example.com");

    user.setId("owner-1");

    return user;
  }
}
