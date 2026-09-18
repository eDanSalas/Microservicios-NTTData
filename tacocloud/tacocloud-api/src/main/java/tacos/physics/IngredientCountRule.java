package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tacos.Taco;

@Component
public class IngredientCountRule implements TacoDesignRule {

  public static final String CODE = "INGREDIENT_COUNT";
  private final int min;
  private final int max;

  public IngredientCountRule(@Value("${tacocloud.physics.min-ingredients:2}") int min,
      @Value("${tacocloud.physics.max-ingredients:12}") int max) {
    this.min = min;
    this.max = max;
  }

  @Override
  public List<DesignViolation> validate(Taco taco) {
    int count = taco.getIngredients().size();
    return count >= min && count <= max ? Collections.emptyList()
        : List.of(new DesignViolation(CODE, "A taco must contain between " + min + " and " + max + " ingredients"));
  }
}
