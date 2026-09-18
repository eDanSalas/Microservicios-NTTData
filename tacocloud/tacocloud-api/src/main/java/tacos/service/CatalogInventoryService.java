package tacos.service;

import java.util.HashSet;

import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import reactor.core.publisher.Mono;
import tacos.Ingredient;
import tacos.data.IngredientRepository;
import tacos.web.api.dto.CatalogUpdateRequest;
import tacos.web.api.dto.StockAdjustmentRequest;

@Service
public class CatalogInventoryService {

  private final IngredientRepository repo;

  public CatalogInventoryService(IngredientRepository repo) {
    this.repo = repo;
  }

  public Mono<Ingredient> updateCatalog(String id, CatalogUpdateRequest request) {
    return find(id).flatMap(ingredient -> {
      verifyVersion(ingredient, request.getVersion());
      if (request.getUnitPrice() != null) ingredient.setUnitPrice(request.getUnitPrice());
      if (request.getAvailable() != null) ingredient.setAvailable(request.getAvailable());
      if (request.getReorderLevel() != null) ingredient.setReorderLevel(request.getReorderLevel());
      if (request.getDietaryTags() != null) ingredient.setDietaryTags(new HashSet<>(request.getDietaryTags()));
      if (request.getAllergens() != null) ingredient.setAllergens(new HashSet<>(request.getAllergens()));
      if (request.getSpiceLevel() != null) ingredient.setSpiceLevel(request.getSpiceLevel());
      validate(ingredient);
      return save(ingredient);
    });
  }

  public Mono<Ingredient> adjustStock(String id, StockAdjustmentRequest request) {
    return find(id).flatMap(ingredient -> {
      verifyVersion(ingredient, request.getVersion());
      int stock;
      try {
        stock = Math.addExact(ingredient.getStockOnHand(), request.getQuantity());
      } catch (ArithmeticException exception) {
        throw businessError("Stock adjustment is outside the supported range");
      }
      if (request.getQuantity() == 0) throw businessError("Stock adjustment must not be zero");
      if (stock < 0) throw businessError("Stock on hand cannot be negative");
      ingredient.setStockOnHand(stock);
      if (stock == 0) ingredient.setAvailable(false);
      validate(ingredient);
      return save(ingredient);
    });
  }

  private Mono<Ingredient> find(String id) {
    return repo.findById(id).switchIfEmpty(Mono.error(new ResponseStatusException(
        HttpStatus.NOT_FOUND, "Ingredient not found: " + id)));
  }

  private Mono<Ingredient> save(Ingredient ingredient) {
    return repo.save(ingredient).onErrorMap(OptimisticLockingFailureException.class,
        exception -> conflict());
  }

  private void verifyVersion(Ingredient ingredient, Long expectedVersion) {
    if (ingredient.getVersion() == null || !ingredient.getVersion().equals(expectedVersion)) throw conflict();
  }

  private void validate(Ingredient ingredient) {
    if (ingredient.getUnitPrice() == null || ingredient.getUnitPrice().signum() < 0)
      throw businessError("Unit price cannot be negative");
    if (ingredient.getStockOnHand() < 0 || ingredient.getReorderLevel() < 0)
      throw businessError("Stock values cannot be negative");
    if (ingredient.isAvailable() && ingredient.getStockOnHand() == 0)
      throw businessError("An ingredient without stock cannot be available");
  }

  private ResponseStatusException conflict() {
    return new ResponseStatusException(HttpStatus.CONFLICT, "Ingredient version is stale");
  }

  private ResponseStatusException businessError(String detail) {
    return new ResponseStatusException(HttpStatus.UNPROCESSABLE_ENTITY, detail);
  }
}
