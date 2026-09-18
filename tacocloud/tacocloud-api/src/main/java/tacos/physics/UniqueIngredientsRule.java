package tacos.physics;

import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Component;

import tacos.Taco;

@Component
public class UniqueIngredientsRule implements TacoDesignRule {

  public static final String CODE = "DUPLICATE_INGREDIENT";

  @Override
  public List<DesignViolation> validate(Taco taco) {
    Set<String> ids = new HashSet<>();
    boolean duplicate = taco.getIngredients().stream().anyMatch(ingredient -> !ids.add(ingredient.getId()));
    return duplicate ? List.of(new DesignViolation(CODE, "A taco cannot contain duplicate ingredients"))
        : Collections.emptyList();
  }
}
