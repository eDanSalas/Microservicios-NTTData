package tacos.physics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.SpiceLevel;
import tacos.Taco;

public class TacoDesignRulesTest {

  @Test
  public void shouldRequireExactlyOneBase() {
    assertThat(new BaseCountRule().validate(taco(ingredient("W", Type.WRAP)))).isEmpty();
    assertThat(new BaseCountRule().validate(taco(ingredient("W", Type.WRAP), ingredient("B", Type.BOWL))))
        .extracting(DesignViolation::getCode).containsExactly(BaseCountRule.CODE);
  }

  @Test
  public void shouldRequireConfiguredIngredientCount() {
    IngredientCountRule rule = new IngredientCountRule(2, 3);
    assertThat(rule.validate(taco(ingredient("W", Type.WRAP), ingredient("P", Type.PROTEIN)))).isEmpty();
    assertThat(rule.validate(taco(ingredient("W", Type.WRAP))))
        .extracting(DesignViolation::getCode).containsExactly(IngredientCountRule.CODE);
  }

  @Test
  public void shouldRejectDuplicatesAndUnavailableIngredients() {
    Ingredient unavailable = ingredient("P", Type.PROTEIN);
    unavailable.setAvailable(false);
    Taco taco = taco(ingredient("W", Type.WRAP), unavailable, unavailable);

    assertThat(new UniqueIngredientsRule().validate(taco))
        .extracting(DesignViolation::getCode).containsExactly(UniqueIngredientsRule.CODE);
    assertThat(new AvailableIngredientsRule().validate(taco))
        .extracting(DesignViolation::getCode).containsExactly(
            AvailableIngredientsRule.CODE, AvailableIngredientsRule.CODE);
  }

  @Test
  public void shouldRequireBeverageForExtremeSpiceWhenEnabled() {
    Ingredient extreme = ingredient("G", Type.SAUCE);
    extreme.setSpiceLevel(SpiceLevel.EXTREME);
    Taco taco = taco(ingredient("W", Type.WRAP), extreme);

    assertThat(new ExtremeSpiceRequiresBeverageRule(true).validate(taco))
        .extracting(DesignViolation::getCode).containsExactly(ExtremeSpiceRequiresBeverageRule.CODE);
    assertThat(new ExtremeSpiceRequiresBeverageRule(false).validate(taco)).isEmpty();
    taco.getIngredients().add(ingredient("D", Type.BEVERAGE));
    assertThat(new ExtremeSpiceRequiresBeverageRule(true).validate(taco)).isEmpty();
  }

  @Test
  public void shouldApplyConfiguredSauceLimit() {
    Taco taco = taco(ingredient("W", Type.WRAP), ingredient("S1", Type.SAUCE), ingredient("S2", Type.SAUCE));
    assertThat(new SauceLimitRule(1).validate(taco))
        .extracting(DesignViolation::getCode).containsExactly(SauceLimitRule.CODE);
    assertThat(new SauceLimitRule(2).validate(taco)).isEmpty();
  }

  private Taco taco(Ingredient... ingredients) {
    Taco taco = new Taco();
    taco.setIngredients(new java.util.ArrayList<>(List.of(ingredients)));
    return taco;
  }

  private Ingredient ingredient(String id, Type type) {
    Ingredient ingredient = new Ingredient(id, id, type);
    ingredient.setAvailable(true);
    return ingredient;
  }
}
