package tacos.web.api;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.time.LocalDate;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.http.MediaType;
import org.springframework.test.web.reactive.server.WebTestClient;
import org.springframework.test.web.servlet.client.MockMvcWebTestClient;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.SpiceLevel;
import tacos.Taco;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;
import tacos.data.TacoSearchPage;
import tacos.data.TacoSearchQuery;
import tacos.service.TacoClassificationService;
import tacos.service.TacoDesignService;
import tacos.service.TacoOfTheDayService;
import tacos.service.TacoRecommendation;
import tacos.physics.AvailableIngredientsRule;
import tacos.physics.BaseCountRule;
import tacos.physics.ExtremeSpiceRequiresBeverageRule;
import tacos.physics.IngredientCountRule;
import tacos.physics.SauceLimitRule;
import tacos.physics.TacoDesignValidator;
import tacos.physics.UniqueIngredientsRule;
import tacos.web.api.error.GlobalApiExceptionHandler;
import tacos.web.api.mapper.IngredientMapper;
import tacos.web.api.mapper.TacoMapper;

public class TacoControllerTest {

  private TacoRepository tacoRepo;
  private IngredientRepository ingredientRepo;
  private WebTestClient client;
  private TacoOfTheDayService tacoOfTheDayService;

  @BeforeEach
  public void setUp() {
    tacoRepo = Mockito.mock(TacoRepository.class);
    ingredientRepo = Mockito.mock(IngredientRepository.class);
    TacoClassificationService classification = new TacoClassificationService();
    TacoMapper mapper = new TacoMapper(new IngredientMapper(), classification);
    TacoDesignService designs = new TacoDesignService(ingredientRepo, validator());
    tacoOfTheDayService = Mockito.mock(TacoOfTheDayService.class);
    client = MockMvcWebTestClient.bindToController(new TacoController(tacoRepo, mapper, classification, designs,
        new TacoSearchQueryFactory(50, 60), tacoOfTheDayService))
        .controllerAdvice(new GlobalApiExceptionHandler()).build();
  }

  @Test
  public void shouldReturnTacoOfTheDay() {
    LocalDate date = LocalDate.of(2026, 9, 20);
    when(tacoOfTheDayService.recommend())
        .thenReturn(Mono.just(new TacoRecommendation(testTaco(1L), date, "Porque hoy es " + date)));

    client.get().uri("/api/tacos/today").exchange().expectStatus().isOk().expectBody()
        .jsonPath("$.taco.id").isEqualTo("1").jsonPath("$.date").isEqualTo("2026-09-20")
        .jsonPath("$.reason").isEqualTo("Porque hoy es 2026-09-20");
  }

  @Test
  public void shouldReturnNotFoundWhenThereIsNoTacoOfTheDay() {
    when(tacoOfTheDayService.recommend()).thenReturn(Mono.error(
        new ResponseStatusException(org.springframework.http.HttpStatus.NOT_FOUND,
            "No available taco candidates")));

    client.get().uri("/api/tacos/today").exchange().expectStatus().isNotFound();
  }

  @Test
  public void shouldSearchWithFiltersAndPaginationMetadata() {
    Taco taco = testTaco(1L);
    when(tacoRepo.search(any(TacoSearchQuery.class)))
        .thenReturn(Mono.just(new TacoSearchPage(List.of(taco), 1, 5, 6, 2)));

    client.get().uri("/api/tacos?name=Taco&ingredientId=INGA&diet=vegan&excludeAllergen=peanut"
        + "&spice=none&page=1&size=5&sort=name,asc").exchange().expectStatus().isOk()
        .expectBody().jsonPath("$.content[0].id").isEqualTo("1").jsonPath("$.page").isEqualTo(1)
        .jsonPath("$.size").isEqualTo(5).jsonPath("$.totalElements").isEqualTo(6)
        .jsonPath("$.totalPages").isEqualTo(2).jsonPath("$.sort").isEqualTo("name,asc");
    verify(tacoRepo).search(org.mockito.ArgumentMatchers.argThat(query -> query.getDiet() == DietaryTag.VEGAN
        && query.getExcludeAllergen() == Allergen.PEANUT && query.getSpice() == SpiceLevel.NONE));
  }

  @Test
  public void shouldCapPageSizeAndRejectUnsupportedSort() {
    when(tacoRepo.search(any(TacoSearchQuery.class))).thenAnswer(invocation -> {
      TacoSearchQuery query = invocation.getArgument(0);
      return Mono.just(new TacoSearchPage(List.of(), query.getPage(), query.getSize(), 0, 0));
    });

    client.get().uri("/api/tacos?size=500").exchange().expectStatus().isOk()
        .expectBody().jsonPath("$.size").isEqualTo(50).jsonPath("$.content").isArray();
    client.get().uri("/api/tacos?sort=ingredients,asc").exchange().expectStatus().isBadRequest();
  }

  @Test
  public void shouldReturnRecentTacos() {
    Taco[] tacos = {
        testTaco(1L), testTaco(2L), testTaco(3L), testTaco(4L),
        testTaco(5L), testTaco(6L), testTaco(7L), testTaco(8L),
        testTaco(9L), testTaco(10L), testTaco(11L), testTaco(12L),
        testTaco(13L), testTaco(14L), testTaco(15L), testTaco(16L)};
    when(tacoRepo.findAll()).thenReturn(Flux.just(tacos));

    client.get().uri("/api/tacos?recent").exchange().expectStatus().isOk().expectBody()
        .jsonPath("$").isArray().jsonPath("$").isNotEmpty()
        .jsonPath("$[0].id").isEqualTo(tacos[0].getId()).jsonPath("$[0].name").isEqualTo("Taco 1")
        .jsonPath("$[11].id").isEqualTo(tacos[11].getId()).jsonPath("$[11].name").isEqualTo("Taco 12")
        .jsonPath("$[0].classification.spiceLevel").isEqualTo("NONE").jsonPath("$[12]").doesNotExist();
  }

  @Test
  public void shouldSaveATacoFromTrustedIngredients() {
    Ingredient wrap = ingredient("INGA", Type.WRAP);
    Ingredient protein = ingredient("INGB", Type.PROTEIN);
    when(ingredientRepo.findById("INGA")).thenReturn(Mono.just(wrap));
    when(ingredientRepo.findById("INGB")).thenReturn(Mono.just(protein));
    when(tacoRepo.save(any())).thenAnswer(invocation -> {
      Taco taco = invocation.getArgument(0);
      taco.setId("TESTID");
      return Mono.just(taco);
    });

    client.post().uri("/api/tacos").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"Test taco\",\"ingredientIds\":[\"INGA\",\"INGB\"]}").exchange()
        .expectStatus().isCreated().expectBody().jsonPath("$.id").isEqualTo("TESTID")
        .jsonPath("$.ingredients[0].id").isEqualTo("INGA")
        .jsonPath("$.classification.dietaryTags[0]").isEqualTo("VEGAN");
  }

  @Test
  public void shouldRejectClientClassification() {
    client.post().uri("/api/tacos").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"Fake taco\",\"ingredientIds\":[\"INGA\"],\"dietaryTags\":[\"VEGAN\"]}")
        .exchange().expectStatus().isBadRequest();

    verifyNoInteractions(tacoRepo, ingredientRepo);
  }

  @Test
  public void shouldReturnDerivedClassification() {
    Taco taco = testTaco(1L);
    taco.getIngredients().get(0).setAllergens(Set.of(Allergen.GLUTEN));
    taco.getIngredients().get(1).setSpiceLevel(SpiceLevel.HOT);
    when(tacoRepo.findById("1")).thenReturn(Mono.just(taco));

    client.get().uri("/api/tacos/1/classification").exchange().expectStatus().isOk().expectBody()
        .jsonPath("$.dietaryTags").isArray().jsonPath("$.allergens[0]").isEqualTo("GLUTEN")
        .jsonPath("$.spiceLevel").isEqualTo("HOT").jsonPath("$.disclaimer").isNotEmpty();
  }

  @Test
  public void shouldReturnAllDesignViolationsAndQueryDuplicateOnce() {
    Ingredient wrap = ingredient("INGA", Type.WRAP);
    when(ingredientRepo.findById("INGA")).thenReturn(Mono.just(wrap));

    client.post().uri("/api/tacos/validate").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"Bad taco\",\"ingredientIds\":[\"INGA\",\"INGA\"]}").exchange()
        .expectStatus().isOk().expectBody().jsonPath("$.valid").isEqualTo(false)
        .jsonPath("$.violations.length()").isEqualTo(2)
        .jsonPath("$.violations[0].code").isEqualTo("BASE_COUNT")
        .jsonPath("$.violations[1].code").isEqualTo("DUPLICATE_INGREDIENT");
    verify(ingredientRepo, times(1)).findById("INGA");
  }

  @Test
  public void shouldRejectInvalidDesignBeforeSaving() {
    Ingredient wrap = ingredient("INGA", Type.WRAP);
    when(ingredientRepo.findById("INGA")).thenReturn(Mono.just(wrap));

    client.post().uri("/api/tacos").contentType(MediaType.APPLICATION_JSON)
        .bodyValue("{\"name\":\"Bad taco\",\"ingredientIds\":[\"INGA\",\"INGA\"]}").exchange()
        .expectStatus().isEqualTo(422).expectBody().jsonPath("$.violations.length()").isEqualTo(2)
        .jsonPath("$.violations[0].field").isEqualTo("BASE_COUNT")
        .jsonPath("$.violations[1].field").isEqualTo("DUPLICATE_INGREDIENT");
    verify(tacoRepo, never()).save(any());
  }

  private Taco testTaco(Long number) {
    Taco taco = new Taco();
    taco.setId(number != null ? number.toString() : "TESTID");
    taco.setName("Taco " + number);
    taco.setIngredients(new ArrayList<>(List.of(ingredient("INGA", Type.WRAP), ingredient("INGB", Type.PROTEIN))));
    return taco;
  }

  private Ingredient ingredient(String id, Type type) {
    Ingredient ingredient = new Ingredient(id, "Ingredient " + id, type);
    ingredient.setDietaryTags(Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE));
    ingredient.setAvailable(true);
    return ingredient;
  }

  private TacoDesignValidator validator() {
    return new TacoDesignValidator(List.of(new BaseCountRule(), new IngredientCountRule(2, 12),
        new UniqueIngredientsRule(), new AvailableIngredientsRule(),
        new ExtremeSpiceRequiresBeverageRule(true), new SauceLimitRule(3)));
  }
}
