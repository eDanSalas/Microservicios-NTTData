package tacos.web.api.mapper;

import java.util.Collections;
import java.util.HashSet;

import org.springframework.stereotype.Component;

import tacos.Ingredient;
import tacos.web.api.dto.AdminIngredientResponse;
import tacos.web.api.dto.IngredientRequest;
import tacos.web.api.dto.IngredientResponse;

@Component
public class IngredientMapper {

  public Ingredient toEntity(
      IngredientRequest request) {

    Ingredient ingredient = new Ingredient();

    ingredient.setName(request.getName());
    ingredient.setType(request.getType());

    return ingredient;
  }

  public void updateEntity(
      IngredientRequest request,
      Ingredient ingredient) {

    ingredient.setName(request.getName());
    ingredient.setType(request.getType());
  }

  public IngredientResponse toResponse(
      Ingredient ingredient) {

    return new IngredientResponse(
        ingredient.getId(),
        ingredient.getName(),
        ingredient.getType(),
        ingredient.getUnitPrice(),
        ingredient.isAvailable(),
        copy(ingredient.getDietaryTags()),
        copy(ingredient.getAllergens()),
        ingredient.getSpiceLevel());
  }

  public AdminIngredientResponse toAdminResponse(Ingredient ingredient) {
    return new AdminIngredientResponse(
        ingredient.getId(), ingredient.getName(), ingredient.getType(),
        ingredient.getUnitPrice(), ingredient.isAvailable(), ingredient.getStockOnHand(),
        ingredient.getReorderLevel(), ingredient.getVersion(), copy(ingredient.getDietaryTags()),
        copy(ingredient.getAllergens()), ingredient.getSpiceLevel());
  }

  private <T> java.util.Set<T> copy(java.util.Set<T> values) {
    return values == null ? Collections.emptySet() : new HashSet<>(values);
  }
}
