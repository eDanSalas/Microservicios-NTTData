package tacos.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.Taco;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;
import tacos.physics.AvailableIngredientsRule;
import tacos.physics.BaseCountRule;
import tacos.physics.ExtremeSpiceRequiresBeverageRule;
import tacos.physics.IngredientCountRule;
import tacos.physics.SauceLimitRule;
import tacos.physics.TacoDesignValidator;
import tacos.physics.UniqueIngredientsRule;

public class TacoOfTheDayServiceTest {
  private static final ZoneId ZONE = ZoneId.of("America/Mexico_City");
  private TacoRepository tacoRepo;
  private IngredientRepository ingredientRepo;
  private Map<String, Ingredient> ingredients;
  private Taco alpha;
  private Taco beta;

  @BeforeEach
  public void setUp() {
    tacoRepo = Mockito.mock(TacoRepository.class);
    ingredientRepo = Mockito.mock(IngredientRepository.class);
    ingredients = new HashMap<>();
    alpha = taco("A", "WA", "PA");
    beta = taco("B", "WB", "PB");
    when(ingredientRepo.findById(anyString()))
        .thenAnswer(invocation -> Mono.justOrEmpty(ingredients.get(invocation.getArgument(0))));
  }

  @Test
  public void shouldReturnSameTacoOnSameDateAndReuseDailyCache() {
    when(tacoRepo.findAll()).thenReturn(Flux.just(beta, alpha));
    when(tacoRepo.findById(anyString())).thenAnswer(invocation -> Mono.just(
        "A".equals(invocation.getArgument(0)) ? alpha : beta));
    TacoOfTheDayService service = service(LocalDate.of(2026, 9, 20));

    String first = service.recommend().block().getTaco().getId();
    String second = service.recommend().block().getTaco().getId();

    assertEquals(first, second);
    verify(tacoRepo, times(1)).findAll();
  }

  @Test
  public void shouldSelectPredictablyForTwoDates() {
    when(tacoRepo.findAll()).thenReturn(Flux.just(beta, alpha));
    LocalDate firstDate = LocalDate.of(2026, 9, 20);
    LocalDate secondDate = firstDate.plusDays(1);

    String first = service(firstDate).recommend().block().getTaco().getId();
    String second = service(secondDate).recommend().block().getTaco().getId();

    assertEquals(expected(firstDate), first);
    assertEquals(expected(secondDate), second);
    assertNotEquals(first, second);
  }

  @Test
  public void shouldIgnorePhysicalCatalogOrder() {
    TacoRepository reversedRepo = Mockito.mock(TacoRepository.class);
    when(tacoRepo.findAll()).thenReturn(Flux.just(alpha, beta));
    when(reversedRepo.findAll()).thenReturn(Flux.just(beta, alpha));
    LocalDate date = LocalDate.of(2026, 9, 20);

    String ordered = service(tacoRepo, date).recommend().block().getTaco().getId();
    String reversed = service(reversedRepo, date).recommend().block().getTaco().getId();

    assertEquals(ordered, reversed);
  }

  @Test
  public void shouldReturnNotFoundWithoutCandidates() {
    when(tacoRepo.findAll()).thenReturn(Flux.empty());

    assertThrows(ResponseStatusException.class,
        () -> service(LocalDate.of(2026, 9, 20)).recommend().block());
  }

  @Test
  public void shouldInvalidateUnavailableCachedTaco() {
    when(tacoRepo.findAll()).thenReturn(Flux.just(alpha), Flux.just(beta));
    when(tacoRepo.findById("A")).thenReturn(Mono.just(alpha));
    TacoOfTheDayService service = service(LocalDate.of(2026, 9, 20));
    assertEquals("A", service.recommend().block().getTaco().getId());
    ingredients.get("WA").setAvailable(false);

    assertEquals("B", service.recommend().block().getTaco().getId());
    verify(tacoRepo, times(2)).findAll();
  }

  private TacoOfTheDayService service(LocalDate date) {
    return service(tacoRepo, date);
  }

  private TacoOfTheDayService service(TacoRepository repository, LocalDate date) {
    Clock clock = Clock.fixed(date.atStartOfDay(ZONE).toInstant(), ZoneId.of("UTC"));
    return new TacoOfTheDayService(repository, ingredientRepo, validator(), clock, ZONE);
  }

  private String expected(LocalDate date) {
    return List.of("A", "B").get(Math.floorMod(date.toEpochDay(), 2));
  }

  private Taco taco(String id, String wrapId, String proteinId) {
    Taco taco = new Taco();
    taco.setId(id);
    taco.setName("Taco " + id);
    taco.setIngredients(List.of(ingredient(wrapId, Type.WRAP), ingredient(proteinId, Type.PROTEIN)));
    return taco;
  }

  private Ingredient ingredient(String id, Type type) {
    Ingredient ingredient = new Ingredient(id, id, type);
    ingredient.setAvailable(true);
    ingredients.put(id, ingredient);
    return ingredient;
  }

  private TacoDesignValidator validator() {
    return new TacoDesignValidator(List.of(new BaseCountRule(), new IngredientCountRule(2, 12),
        new UniqueIngredientsRule(), new AvailableIngredientsRule(),
        new ExtremeSpiceRequiresBeverageRule(true), new SauceLimitRule(3)));
  }
}
