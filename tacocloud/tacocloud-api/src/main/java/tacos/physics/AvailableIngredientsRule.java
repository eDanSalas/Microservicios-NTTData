package tacos.physics;

import java.util.List;
import java.util.stream.Collectors;

import org.springframework.stereotype.Component;

import tacos.Taco;

@Component
public class AvailableIngredientsRule implements TacoDesignRule {

  public static final String CODE = "INGREDIENT_UNAVAILABLE";

  @Override
  public List<DesignViolation> validate(Taco taco) {
    return taco.getIngredients().stream().filter(ingredient -> !ingredient.isAvailable())
        .map(ingredient -> new DesignViolation(CODE, "Ingredient is unavailable: " + ingredient.getId()))
        .collect(Collectors.toList());
  }
}
