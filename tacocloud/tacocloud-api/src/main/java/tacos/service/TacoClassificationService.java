package tacos.service;

import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Set;

import org.springframework.stereotype.Service;

import tacos.Allergen;
import tacos.DietaryTag;
import tacos.Ingredient;
import tacos.SpiceLevel;
import tacos.Taco;
import tacos.web.api.dto.TacoClassificationResponse;

@Service
public class TacoClassificationService {

  public static final String DISCLAIMER = "Academic metadata does not replace real cross-contamination controls";

  public TacoClassificationResponse classify(Taco taco) {
    List<Ingredient> ingredients = taco == null || taco.getIngredients() == null
        ? Collections.emptyList() : taco.getIngredients();
    Set<DietaryTag> tags = EnumSet.noneOf(DietaryTag.class);
    if (!ingredients.isEmpty()) {
      for (DietaryTag tag : DietaryTag.values())
        if (ingredients.stream().allMatch(ingredient -> supports(ingredient, tag))) tags.add(tag);
    }
    Set<Allergen> allergens = EnumSet.noneOf(Allergen.class);
    ingredients.stream().filter(ingredient -> ingredient.getAllergens() != null)
        .forEach(ingredient -> allergens.addAll(ingredient.getAllergens()));
    if (allergens.contains(Allergen.GLUTEN)) tags.remove(DietaryTag.GLUTEN_FREE);
    SpiceLevel spice = ingredients.stream().map(this::spiceLevel)
        .max(Enum::compareTo).orElse(SpiceLevel.NONE);
    return new TacoClassificationResponse(tags, allergens, spice, DISCLAIMER);
  }

  private boolean supports(Ingredient ingredient, DietaryTag tag) {
    Set<DietaryTag> tags = ingredient.getDietaryTags();
    if (tags == null) return false;
    return tags.contains(tag) || tag == DietaryTag.VEGETARIAN && tags.contains(DietaryTag.VEGAN);
  }

  private SpiceLevel spiceLevel(Ingredient ingredient) {
    return ingredient.getSpiceLevel() == null ? SpiceLevel.NONE : ingredient.getSpiceLevel();
  }
}
