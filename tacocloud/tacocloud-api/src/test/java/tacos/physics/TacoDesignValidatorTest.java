package tacos.physics;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;

import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.Taco;

public class TacoDesignValidatorTest {

  @Test
  public void shouldReturnAllViolationsInStableOrder() {
    Ingredient duplicate = new Ingredient("DUP", "Duplicate", Type.PROTEIN);
    Taco taco = new Taco();
    taco.setIngredients(List.of(duplicate, duplicate));
    TacoDesignValidator validator = new TacoDesignValidator(List.of(
        new UniqueIngredientsRule(), new BaseCountRule(), new AvailableIngredientsRule()));

    assertThat(validator.validate(taco)).extracting(DesignViolation::getCode)
        .containsExactly(BaseCountRule.CODE, UniqueIngredientsRule.CODE,
            AvailableIngredientsRule.CODE, AvailableIngredientsRule.CODE);
  }

  @Test
  public void shouldAcceptAnInjectedRuleWithoutChangingValidator() {
    TacoDesignRule fake = taco -> List.of(new DesignViolation("FAKE_RULE", "Fake violation"));
    TacoDesignValidator validator = new TacoDesignValidator(List.of(fake));

    assertThat(validator.validate(new Taco())).containsExactly(new DesignViolation("FAKE_RULE", "Fake violation"));
  }
}
