package tacos.web.api;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;

import tacos.pricing.CouponProperties;
import tacos.pricing.CouponRule;
import tacos.pricing.CouponService;
import tacos.pricing.CouponType;
import tacos.service.OrderPricingService;
import tacos.service.TacoClassificationService;
import tacos.service.TacoDesignService;
import tacos.physics.AvailableIngredientsRule;
import tacos.physics.BaseCountRule;
import tacos.physics.ExtremeSpiceRequiresBeverageRule;
import tacos.physics.IngredientCountRule;
import tacos.physics.SauceLimitRule;
import tacos.physics.TacoDesignValidator;
import tacos.physics.UniqueIngredientsRule;
import tacos.data.IngredientRepository;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import reactor.core.publisher.Mono;
import tacos.web.api.error.GlobalApiExceptionHandler;

public class OrderQuoteControllerTest {

  private WebTestClient client;
  private IngredientRepository ingredientRepo;

  @BeforeEach
  public void setUp() {
    CouponRule rule = new CouponRule();
    rule.setType(CouponType.PERCENTAGE);
    rule.setValue(new BigDecimal("10"));
    CouponProperties properties = new CouponProperties();
    properties.setCodes(Map.of("SECRET10", rule));
    CouponService coupons = new CouponService(properties,
        Clock.fixed(Instant.parse("2026-06-15T12:00:00Z"), ZoneOffset.UTC));
    ingredientRepo = Mockito.mock(IngredientRepository.class);
    TacoDesignValidator validator = new TacoDesignValidator(java.util.List.of(new BaseCountRule(),
        new IngredientCountRule(2, 12), new UniqueIngredientsRule(), new AvailableIngredientsRule(),
        new ExtremeSpiceRequiresBeverageRule(true), new SauceLimitRule(3)));
    client = WebTestClient.bindToController(new OrderQuoteController(coupons,
        new OrderPricingService(20, "MXN"), new TacoClassificationService(),
        new TacoDesignService(ingredientRepo, validator)))
        .controllerAdvice(new GlobalApiExceptionHandler()).build();
  }

  @Test
  public void shouldQuoteValidCoupon() {
    client.post().uri("/api/orders/quote").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"subtotal\":100.00,\"couponCode\":\"secret10\"}").exchange()
        .expectStatus().isOk().expectBody().jsonPath("$.couponCode").isEqualTo("SECRET10")
        .jsonPath("$.discount").isEqualTo(10.0).jsonPath("$.total").isEqualTo(90.0)
        .jsonPath("$.currency").isEqualTo("MXN").jsonPath("$.classifications").isEmpty();
  }

  @Test
  public void shouldIncludeDerivedClassificationInQuote() {
    Ingredient ingredient = new Ingredient("VEGN", "Vegan filling", Type.PROTEIN);
    ingredient.setDietaryTags(java.util.Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE));
    ingredient.setAvailable(true);
    Ingredient wrap = new Ingredient("WRAP", "Wrap", Type.WRAP);
    wrap.setDietaryTags(java.util.Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE));
    wrap.setAvailable(true);
    when(ingredientRepo.findById("VEGN")).thenReturn(Mono.just(ingredient));
    when(ingredientRepo.findById("WRAP")).thenReturn(Mono.just(wrap));

    client.post().uri("/api/orders/quote").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"subtotal\":100.00,\"couponCode\":\"secret10\",\"items\":[{\"quantity\":1,"
            + "\"taco\":{\"name\":\"Vegan taco\",\"ingredientIds\":[\"WRAP\",\"VEGN\"]}}]}").exchange()
        .expectStatus().isOk().expectBody().jsonPath("$.classifications[0].dietaryTags[0]").isEqualTo("VEGAN")
        .jsonPath("$.classifications[0].spiceLevel").isEqualTo("NONE");
  }

  @Test
  public void shouldRejectInvalidDesignBeforeQuote() {
    Ingredient protein = new Ingredient("PROT", "Protein", Type.PROTEIN);
    protein.setAvailable(true);
    Ingredient sauce = new Ingredient("SAUC", "Sauce", Type.SAUCE);
    sauce.setAvailable(true);
    when(ingredientRepo.findById("PROT")).thenReturn(Mono.just(protein));
    when(ingredientRepo.findById("SAUC")).thenReturn(Mono.just(sauce));

    client.post().uri("/api/orders/quote").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"subtotal\":100.00,\"couponCode\":\"secret10\",\"items\":[{\"quantity\":1,"
            + "\"taco\":{\"name\":\"Invalid taco\",\"ingredientIds\":[\"PROT\",\"SAUC\"]}}]}")
        .exchange().expectStatus().isEqualTo(422).expectBody()
        .jsonPath("$.violations[0].field").isEqualTo("BASE_COUNT");
  }

  @Test
  public void shouldNotEnumerateCouponCodes() {
    client.post().uri("/api/orders/quote").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"subtotal\":100.00,\"couponCode\":\"UNKNOWN\"}").exchange()
        .expectStatus().isEqualTo(422).expectBody(String.class)
        .value(body -> assertFalse(body.contains("SECRET10")));
  }
}
