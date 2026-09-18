package tacos.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Set;

import org.junit.jupiter.api.Test;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.Ingredient.Type;
import tacos.SpiceLevel;
import tacos.Taco;
import tacos.web.api.dto.TacoClassificationResponse;

public class TacoClassificationServiceTest {

  private final TacoClassificationService service = new TacoClassificationService();

  @Test
  public void shouldComposeDietaryTagsFromEveryIngredient() {
    TacoClassificationResponse vegan = service.classify(taco(
        ingredient(Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE), Set.of(), SpiceLevel.NONE),
        ingredient(Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE), Set.of(), SpiceLevel.MILD)));
    TacoClassificationResponse vegetarian = service.classify(taco(
        ingredient(Set.of(DietaryTag.VEGAN, DietaryTag.GLUTEN_FREE), Set.of(), SpiceLevel.NONE),
        ingredient(Set.of(DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE), Set.of(), SpiceLevel.NONE)));

    assertThat(vegan.getDietaryTags()).containsExactlyInAnyOrder(
        DietaryTag.VEGAN, DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE);
    assertThat(vegetarian.getDietaryTags()).containsExactlyInAnyOrder(
        DietaryTag.VEGETARIAN, DietaryTag.GLUTEN_FREE).doesNotContain(DietaryTag.VEGAN);
  }

  @Test
  public void shouldUnionAllergensAndUseHighestSpiceLevel() {
    TacoClassificationResponse result = service.classify(taco(
        ingredient(Set.of(DietaryTag.GLUTEN_FREE), Set.of(Allergen.DAIRY, Allergen.SOY), SpiceLevel.MILD),
        ingredient(Set.of(DietaryTag.GLUTEN_FREE), Set.of(Allergen.PEANUT), SpiceLevel.EXTREME)));

    assertThat(result.getAllergens()).containsExactlyInAnyOrder(Allergen.DAIRY, Allergen.SOY, Allergen.PEANUT);
    assertThat(result.getSpiceLevel()).isEqualTo(SpiceLevel.EXTREME);
    assertThat(result.getDisclaimer()).isEqualTo(TacoClassificationService.DISCLAIMER);
  }

  @Test
  public void shouldNotClaimGlutenFreeWhenGlutenIsPresent() {
    TacoClassificationResponse result = service.classify(taco(
        ingredient(Set.of(DietaryTag.GLUTEN_FREE), Set.of(Allergen.GLUTEN), SpiceLevel.NONE)));

    assertThat(result.getDietaryTags()).doesNotContain(DietaryTag.GLUTEN_FREE);
  }

  private Taco taco(Ingredient... ingredients) {
    Taco taco = new Taco();
    taco.setIngredients(List.of(ingredients));
    return taco;
  }

  private Ingredient ingredient(Set<DietaryTag> tags, Set<Allergen> allergens, SpiceLevel spice) {
    Ingredient ingredient = new Ingredient("TEST", "Test", Type.PROTEIN);
    ingredient.setDietaryTags(tags);
    ingredient.setAllergens(allergens);
    ingredient.setSpiceLevel(spice);
    return ingredient;
  }
}
