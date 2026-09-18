package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tacos.Ingredient.Type;
import tacos.Taco;

@Component
public class SauceLimitRule implements TacoDesignRule {

  public static final String CODE = "SAUCE_LIMIT";
  private final int maxSauces;

  public SauceLimitRule(@Value("${tacocloud.physics.max-sauces:3}") int maxSauces) {
    this.maxSauces = maxSauces;
  }

  @Override
  public List<DesignViolation> validate(Taco taco) {
    long sauces = taco.getIngredients().stream().filter(ingredient -> ingredient.getType() == Type.SAUCE).count();
    return sauces <= maxSauces ? Collections.emptyList()
        : List.of(new DesignViolation(CODE, "A taco can contain at most " + maxSauces + " sauces"));
  }
}
