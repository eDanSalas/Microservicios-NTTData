package tacos.service;

import java.time.Clock;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.Comparator;
import java.util.List;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.Taco;
import tacos.data.IngredientRepository;
import tacos.data.TacoRepository;
import tacos.physics.TacoDesignValidator;

@Service
public class TacoOfTheDayService {
  private final TacoRepository tacoRepo;
  private final IngredientRepository ingredientRepo;
  private final TacoDesignValidator validator;
  private final Clock clock;
  private final ZoneId zone;
  private volatile CachedRecommendation cache;

  public TacoOfTheDayService(TacoRepository tacoRepo, IngredientRepository ingredientRepo,
      TacoDesignValidator validator, @Qualifier("tacoRecommendationClock") Clock clock,
      @Qualifier("tacoRecommendationZone") ZoneId zone) {
    this.tacoRepo = tacoRepo;
    this.ingredientRepo = ingredientRepo;
    this.validator = validator;
    this.clock = clock;
    this.zone = zone;
  }

  public Mono<TacoRecommendation> recommend() {
    LocalDate date = LocalDate.now(clock.withZone(zone));
    CachedRecommendation current = cache;
    if (current == null || !current.date.equals(date)) return select(date);
    return tacoRepo.findById(current.tacoId).flatMap(this::eligible)
        .map(taco -> recommendation(taco, date)).switchIfEmpty(Mono.defer(() -> select(date)));
  }

  private Mono<TacoRecommendation> select(LocalDate date) {
    return tacoRepo.findAll().flatMap(this::eligible).sort(Comparator.comparing(Taco::getId))
        .collectList().flatMap(candidates -> candidate(candidates, date)).map(taco -> {
          cache = new CachedRecommendation(date, taco.getId());
          return recommendation(taco, date);
        });
  }

  private Mono<Taco> candidate(List<Taco> candidates, LocalDate date) {
    if (candidates.isEmpty()) return Mono.error(new ResponseStatusException(HttpStatus.NOT_FOUND,
        "No available taco candidates"));
    return Mono.just(candidates.get(Math.floorMod(date.toEpochDay(), candidates.size())));
  }

  private Mono<Taco> eligible(Taco taco) {
    if (taco == null || taco.getId() == null || taco.getIngredients() == null) return Mono.empty();
    return Flux.fromIterable(taco.getIngredients()).concatMap(this::currentIngredient).collectList()
        .filter(ingredients -> ingredients.size() == taco.getIngredients().size())
        .map(ingredients -> resolved(taco, ingredients))
        .filter(candidate -> validator.validate(candidate).isEmpty());
  }

  private Mono<Ingredient> currentIngredient(Ingredient ingredient) {
    return ingredient == null || ingredient.getId() == null ? Mono.empty()
        : ingredientRepo.findById(ingredient.getId());
  }

  private Taco resolved(Taco taco, List<Ingredient> ingredients) {
    Taco resolved = new Taco();
    resolved.setId(taco.getId());
    resolved.setName(taco.getName());
    resolved.setCreatedAt(taco.getCreatedAt());
    resolved.setIngredients(ingredients);
    return resolved;
  }

  private TacoRecommendation recommendation(Taco taco, LocalDate date) {
    return new TacoRecommendation(taco, date, "Porque hoy es " + date + " en " + zone);
  }

  private static class CachedRecommendation {
    private final LocalDate date;
    private final String tacoId;

    private CachedRecommendation(LocalDate date, String tacoId) {
      this.date = date;
      this.tacoId = tacoId;
    }
  }
}
