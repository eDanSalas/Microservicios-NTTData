package tacos.physics;

import java.util.Collections;
import java.util.List;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import tacos.Ingredient.Type;
import tacos.SpiceLevel;
import tacos.Taco;

@Component
public class ExtremeSpiceRequiresBeverageRule implements TacoDesignRule {

  public static final String CODE = "EXTREME_SPICE_REQUIRES_BEVERAGE";
  private final boolean enabled;

  public ExtremeSpiceRequiresBeverageRule(
      @Value("${tacocloud.physics.extreme-spice-requires-beverage:true}") boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public List<DesignViolation> validate(Taco taco) {
    boolean extreme = taco.getIngredients().stream()
        .anyMatch(ingredient -> ingredient.getSpiceLevel() == SpiceLevel.EXTREME);
    boolean beverage = taco.getIngredients().stream().anyMatch(ingredient -> ingredient.getType() == Type.BEVERAGE);
    return !enabled || !extreme || beverage ? Collections.emptyList()
        : List.of(new DesignViolation(CODE, "Extreme spice requires a beverage"));
  }
}
