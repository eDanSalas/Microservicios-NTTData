package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.stereotype.Component;

import tacos.Ingredient.Type;
import tacos.Taco;

@Component
public class BaseCountRule implements TacoDesignRule {

  public static final String CODE = "BASE_COUNT";

  @Override
  public List<DesignViolation> validate(Taco taco) {
    long bases = taco.getIngredients().stream()
        .filter(ingredient -> ingredient.getType() == Type.WRAP || ingredient.getType() == Type.BOWL).count();
    return bases == 1 ? Collections.emptyList()
        : List.of(new DesignViolation(CODE, "A taco must contain exactly one wrap or bowl base"));
  }
}
