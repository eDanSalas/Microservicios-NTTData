package tacos.service;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.Taco;
import tacos.data.IngredientRepository;
import tacos.physics.TacoDesignValidator;
import tacos.web.api.dto.TacoValidationResponse;

@Service
public class TacoDesignService {

  private final IngredientRepository ingredientRepo;
  private final TacoDesignValidator validator;

  public TacoDesignService(IngredientRepository ingredientRepo, TacoDesignValidator validator) {
    this.ingredientRepo = ingredientRepo;
    this.validator = validator;
  }

  public Mono<Taco> resolve(String name, List<String> ingredientIds) {
    return Flux.fromIterable(new LinkedHashSet<>(ingredientIds)).concatMap(this::ingredient)
        .collectMap(Ingredient::getId).map(ingredients -> taco(name, ingredientIds, ingredients));
  }

  public Mono<Taco> resolveAndRequireValid(String name, List<String> ingredientIds) {
    return resolve(name, ingredientIds).map(validator::requireValid);
  }

  public Mono<TacoValidationResponse> validate(String name, List<String> ingredientIds) {
    return resolve(name, ingredientIds).map(taco -> {
      List<tacos.physics.DesignViolation> violations = validator.validate(taco);
      return new TacoValidationResponse(violations.isEmpty(), violations);
    });
  }

  private Mono<Ingredient> ingredient(String id) {
    return ingredientRepo.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(
        HttpStatus.UNPROCESSABLE_ENTITY, "Unknown ingredient id: " + id)));
  }

  private Taco taco(String name, List<String> ids, Map<String, Ingredient> ingredients) {
    Taco taco = new Taco();
    taco.setName(name);
    taco.setIngredients(ids.stream().map(ingredients::get).collect(Collectors.toList()));
    return taco;
  }
}
